#include "Timer.h"
#include "NeighborDetection.h"
#include "UserButton.h"

module NeighborDetectionC @safe() {
  uses {
    interface Leds;
    interface Boot;
    interface Receive;
    interface AMSend;
    interface Timer<TMilli> as ExperimentTimeout;
    interface Timer<TMilli> as ExperimentDelay;
    interface SplitControl as AMControl;
    interface Packet as RadioPacket;
    
    interface NeighborDetection;
    interface LowPowerListening;

#ifdef TIMESTAMP_BUTTON
    interface Notify<button_state_t> as Button;
#endif

#ifndef DUPLICATE_REPORTS
    interface Queue<am_addr_t> as DetectedNeighbors;
#endif
    interface Queue<report_t> as ReportBuffer;

    interface Receive as SerialReceive;
    interface AMSend as SerialSend;
    interface SplitControl as SerialControl;
    interface Packet as SerialPacket;
    
    interface HplMsp430GeneralIO as UsbConnection;

    interface Random;
    interface ParameterInit<uint32_t> as SeedInit;
    interface ParameterInit<uint16_t> as SystemSeedInit;

    interface LocalTime<TMilli>;
#ifdef FLASH_LOG
    interface LogWrite;
#endif
  }
}
implementation {

  uint32_t experiment_start;
  message_t serial_pkt, radio_pkt;
  bool report_busy = FALSE, radio_busy = FALSE;
  bool run_experiment = FALSE;
  experiment_ctrl_t experiment;
  uint32_t report_seq = 0;
  report_t report_buf;


  void consume_report();


  void next_report()
  {
    call ReportBuffer.dequeue();
    report_seq++;

    if (!call ReportBuffer.empty())
      consume_report(); 
  }


  void serial_report()
  {
    error_t status;
    report_t *report = 
        (report_t *) call SerialPacket.getPayload(&serial_pkt, 0);
    memcpy(report, &report_buf, sizeof(report_t));
    report->absolute_time = call LocalTime.get();
    
    status = call SerialSend.send(AM_BROADCAST_ADDR, &serial_pkt, 
            sizeof(report_t));

    if (status != SUCCESS) {
      consume_report();
      return;
    }
  }


#ifdef FLASH_LOG
  void log_report()
  {
    report_buf.absolute_time = call LocalTime.get();
    if (call LogWrite.append(&report_buf, sizeof(report_t)) != SUCCESS)
      // TODO if both LogWrite.append and SerialSend.send fail, we might get an
      // ugly recursion here.
      serial_report();
  }


  event void LogWrite.appendDone(void *buf, storage_len_t len, bool recordsLost,
          error_t err)
  {
    serial_report();
  }


  event void LogWrite.syncDone(error_t error)
  {
  }


  event void LogWrite.eraseDone(error_t error)
  {
  }
#endif

  
  void consume_report()
  {
    if (report_busy || call ReportBuffer.empty())
      return;

    report_buf = call ReportBuffer.head();
    report_buf.seq = report_seq;
    report_busy = TRUE;

#ifdef FLASH_LOG
    log_report();
#else
    serial_report();
#endif
  }


  task void send_experiment()
  {
    error_t status;
    void *payload;

    if (radio_busy)
      return;

    payload = call RadioPacket.getPayload(&radio_pkt, 0);
    memcpy(payload, &experiment, sizeof(experiment_ctrl_t));

    status = call AMSend.send(AM_BROADCAST_ADDR, &radio_pkt,
            sizeof(experiment_ctrl_t));

    if (status != SUCCESS) {
      post send_experiment();
      return;
    }
    radio_busy = TRUE;
  }


  bool receive_message(message_t *msg, void *payload, uint8_t len)
  {
    uint16_t experiment_delay;

    if (len != sizeof(experiment_ctrl_t)) 
      return FAIL;
    
#ifndef DUPLICATE_REPORTS
    while (!call DetectedNeighbors.empty())
      call DetectedNeighbors.dequeue();
#endif

    memcpy(&experiment, payload, len);

    call SeedInit.init(experiment.seed);
    call SystemSeedInit.init(experiment.seed);
    
    if (experiment.randomDelay)
      experiment_delay = call Random.rand16() % experiment.delay;
    else
      experiment_delay = experiment.delay;
    
    call ExperimentDelay.startOneShot(experiment.period + experiment_delay);

    if (experiment.countFromMsgRcv)
      experiment_start = call ExperimentTimeout.getNow(); 
   
    // hack to prevent rebroadcast
    if (experiment.noRebroadcast) {
      call AMControl.stop();
      return FAIL;
    }

    return SUCCESS;
  }


  void dummy_start()
  {
    // In this configuration, T/2 is with 17% longer than the default
    // optimization (beacon=167, samples=5).
    experiment_ctrl_t config;
    config.period = 4000;
    config.beacon = 222;
    config.samples = 8;
    config.timeout = 7200000ULL;
    config.delay = 2000;
    config.randomDelay = 1;
    config.countFromMsgRcv = 1;
    config.noRebroadcast = 1;
    config.seed = TOS_NODE_ID;

    receive_message(NULL, (void *) &config, sizeof(experiment_ctrl_t));
  }


  event void Boot.booted() 
  {
    call UsbConnection.selectIOFunc();
    call UsbConnection.makeInput();
    call SerialControl.start();
    call AMControl.start();
   
#ifdef TIMESTAMP_BUTTON
    call Button.enable();
#endif
  }

  task void start_detection()
  {
    call NeighborDetection.start(experiment.period, experiment.beacon,
            experiment.samples);
    call ExperimentTimeout.startOneShot(experiment.timeout);
    if (!experiment.countFromMsgRcv)
      experiment_start = call ExperimentTimeout.getNow(); 
  }

  event void AMControl.startDone(error_t err) {
    if (err != SUCCESS) {
      call AMControl.start();
      return;
    }

    if (run_experiment)
      post start_detection();
#ifdef AUTOSTART
#warning Autostarting the neighbor discovery
    else
      dummy_start();
#endif
  }


  event void AMControl.stopDone(error_t err) 
  {
  }

  
  event void ExperimentDelay.fired()
  {
    call AMControl.start();
    run_experiment = TRUE;
  }


  event void ExperimentTimeout.fired()
  {
    call Leds.set(0xff);
    WDTCTL = 0;
  }

  
  event message_t *Receive.receive(message_t *msg, void *payload, uint8_t len) 
  {
#ifndef AUTOSTART
    receive_message(msg, payload, len);
    call AMControl.stop();
#endif
    return msg;
  }


  event void AMSend.sendDone(message_t *msg, error_t error) 
  {
    radio_busy = FALSE;

    if (error != SUCCESS)
      post send_experiment();
    else
      call AMControl.stop();
  }


  event void * NeighborDetection.getPayload(uint8_t *len)
  {
    *len = 0; 
    return NULL;
  }


  event void NeighborDetection.detected(am_addr_t addr, void *payload, 
          uint8_t len)
  {
#ifndef DUPLICATE_REPORTS
    uint16_t i;
#endif
    report_t report;

    if (!call ExperimentTimeout.isRunning())
      return;

    call Leds.led2Toggle();
#ifndef DUPLICATE_REPORTS
    for (i = 0; i < call DetectedNeighbors.size(); i++)
      if (call DetectedNeighbors.element(i) == addr)
        return;
#endif

#ifdef TEST_USB
    if (!call UsbConnection.get())
      return;
#endif

    report.src = TOS_NODE_ID;
    report.addr = addr;
    report.timestamp = call ExperimentTimeout.getNow() - experiment_start;
    report.absolute_timestamp = call LocalTime.get();
    call ReportBuffer.enqueue(report);
#ifndef DUPLICATE_REPORTS
    call DetectedNeighbors.enqueue(addr);
#endif
    consume_report(); 
  }


  event message_t *SerialReceive.receive(message_t *msg, void *payload,
          uint8_t len)
  {
#ifndef AUTOSTART
    if (receive_message(msg, payload, len) == SUCCESS)
      post send_experiment();
#endif
    return msg;
  }

  
  event void SerialSend.sendDone(message_t *msg, error_t error) 
  {
    report_busy = FALSE;
    next_report();
  }


  event void SerialControl.startDone(error_t err)
  {
    if (err != SUCCESS) {
      call SerialControl.start();
      return;
    }
  }


  event void SerialControl.stopDone(error_t err)
  {
  }


#ifdef TIMESTAMP_BUTTON
  event void Button.notify(button_state_t state)
  {
    report_t report;
    if (state != BUTTON_RELEASED)
      return;
    report.src = TOS_NODE_ID;
    report.addr = TOS_NODE_ID;
    report.timestamp = call ExperimentTimeout.getNow() - experiment_start;
    report.absolute_timestamp = call LocalTime.get();
    call ReportBuffer.enqueue(report);
    call Leds.led1Toggle();
    consume_report();
  }
#endif
}

