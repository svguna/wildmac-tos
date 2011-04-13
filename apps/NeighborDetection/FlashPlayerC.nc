#include "Timer.h"
#include "NeighborDetection.h"

module FlashPlayerC @safe() {
  uses {
    interface Leds;
    interface Boot;
    interface Timer<TMilli> as ReportDelay;
    
    interface Receive as SerialReceive;
    interface AMSend as SerialSend;
    interface SplitControl as SerialControl;
    interface Packet as SerialPacket;
    
    interface LogRead;
    interface LogWrite;
  }
}
implementation {

  message_t serial_pkt;
  report_t report;
  bool flush = FALSE;


  event void Boot.booted() 
  {
    call SerialControl.start();
  }

  void attempt_read()
  {
    if (call LogRead.read(&report, sizeof(report_t)) == SUCCESS) 
      return;
    call Leds.led0Toggle();
    call ReportDelay.startOneShot(1000);
  }


  event void ReportDelay.fired()
  {
    attempt_read();
  }


  event void LogRead.readDone(void *buf, storage_len_t len, error_t err)
  {
    void *payload;
    
    if (len != sizeof(report_t) || buf != &report) {
      call Leds.led1On();
      if (flush)
        call LogWrite.erase();
      return;
    }

    call Leds.led2Toggle();
    payload = call SerialPacket.getPayload(&serial_pkt, 0);
    memcpy(payload, &report, sizeof(report_t));
    if (call SerialSend.send(AM_BROADCAST_ADDR, &serial_pkt, 
                sizeof(report_t)) != SUCCESS) {
      call Leds.led0Toggle();
      attempt_read();
    }
  }

  
  event message_t *SerialReceive.receive(message_t *msg, void *payload,
          uint8_t len)
  {
    report_ctrl_t *report_ctrl = (report_ctrl_t *) payload;
    if (len != sizeof(report_ctrl_t))
      return msg;
    flush = report_ctrl->flush;
    call ReportDelay.startOneShot(report_ctrl->delay);
    return msg;
  }

  
  event void SerialSend.sendDone(message_t *msg, error_t error) 
  {
    attempt_read();
  }


  event void SerialControl.startDone(error_t err)
  {
    if (err != SUCCESS)
      call SerialControl.start();
  }


  event void SerialControl.stopDone(error_t err)
  {
  }


  event void LogRead.seekDone(error_t err)
  {
  }

  
  event void LogWrite.appendDone(void *buf, storage_len_t len, bool recordsLost,
          error_t error)
  {
  }


  event void LogWrite.eraseDone(error_t err)
  {
    call Leds.set(0xff);
  }


  event void LogWrite.syncDone(error_t err)
  {
  }
}

