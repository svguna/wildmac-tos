#include "Timer.h"
#include "NeighborDetection.h"
#ifdef PLATFORM_TREMATEB 
#include "NmeaGga.h"
#include "Nmea.h"
#endif

module NeighborDetectionC @safe() {
  uses {
    interface Leds;
    interface Boot;
    interface Receive;
    interface Timer<TMilli> as ExperimentDelay;
    interface SplitControl as AMControl;
    interface Packet as RadioPacket;
    
    interface NeighborDetection;

#ifdef PLATFORM_TREMATEB 
    interface PersistentStorage as Fram;
    interface Queue<report_t> as ReportBuffer;

    interface SplitControl as GpsControl;
    interface Notify<nmea_raw_t *> as GpsNotify;
    interface NmeaPacket<nmea_gga_msg_t>;
#endif

    interface Random;
    interface ParameterInit<uint32_t> as SeedInit;
    interface ParameterInit<uint16_t> as SystemSeedInit;

    interface LocalTime<TMilli>;
  }
}

implementation {
  enum {
    FRAM_IDLE, 
    FRAM_RESET,
    FRAM_WRITE_HEADER,
    FRAM_WRITE_LOG
  };

  bool report_busy = FALSE;
  bool run_experiment = FALSE;
  experiment_ctrl_t experiment;
  report_t report_buf;
  gps_coordinates_t gps;

  uint16_t fram_state = FRAM_IDLE;
  struct fram_header fram_header;

#ifdef PLATFORM_TREMATEB
  task void init_log();
  task void reset_log();
  void consume_report();
#endif


#ifdef PLATFORM_TREMATEB 
  void next_report()
  {
    call ReportBuffer.dequeue();

    if (!call ReportBuffer.empty())
      consume_report(); 
  }


  void consume_report()
  {
    if (report_busy || call ReportBuffer.empty())
      return;

    report_buf = call ReportBuffer.head();
    report_busy = TRUE;
    
    fram_state = FRAM_WRITE_HEADER;
    fram_header.entries++;
    if (call Fram.write(0, &fram_header, sizeof(struct fram_header)) == SUCCESS)
      return;
    fram_header.entries--;
    report_busy = FALSE;
  }
#endif


  bool receive_message(message_t *msg, void *payload, uint8_t len)
  {
    if (len != sizeof(experiment_ctrl_t)) 
      return FAIL;
    
    memcpy(&experiment, payload, len);

    call SeedInit.init(experiment.seed);
    call SystemSeedInit.init(experiment.seed);
    
    call ExperimentDelay.startOneShot(2000);

    call AMControl.stop();
    return SUCCESS;
  }



  event void Boot.booted() 
  {
#ifdef PLATFORM_TREMATEB 
    post init_log();
#else
    #warning starting radio
    call AMControl.start();
#endif
  }


#ifdef PLATFORM_TREMATEB 
  task void init_log()
  {
    if (call Fram.read(0, &fram_header, sizeof(struct fram_header)) != SUCCESS)
      post init_log();
  }
#endif


  task void start_detection()
  {
    call NeighborDetection.setBeaconPower(experiment.power);
    call NeighborDetection.start(experiment.period, experiment.beacon,
            experiment.samples);
  }


  event void AMControl.startDone(error_t err) 
  {
    experiment_ctrl_t dummy_start;

    if (err != SUCCESS) {
      call AMControl.start();
      return;
    }

    if (run_experiment) {
      post start_detection();
      return;
    }

    dummy_start.period = 2000;
    dummy_start.beacon = 167;
    dummy_start.samples = 5;
    dummy_start.seed = TOS_NODE_ID;
    dummy_start.power = 3;
    
    receive_message(NULL, &dummy_start, sizeof(experiment_ctrl_t));
  }


  event void AMControl.stopDone(error_t err) 
  {
  }

  
  event void ExperimentDelay.fired()
  {
    call AMControl.start();
    run_experiment = TRUE;
  }


  event message_t *Receive.receive(message_t *msg, void *payload, uint8_t len) 
  {
    receive_message(msg, payload, len);
    return msg;
  }


  event void * NeighborDetection.getPayload(uint8_t *len)
  {
    *len = 0; 
    return NULL;
  }


  event void NeighborDetection.detected(am_addr_t addr, void *payload, 
          uint8_t len)
  {
#ifdef PLATFORM_TREMATEB 
    report_t report;
#endif

    if (!run_experiment)
      return;

#ifdef PLATFORM_TREMATEB 
    report.src = TOS_NODE_ID;
    report.addr = addr;
    report.timestamp = call LocalTime.get();
    memcpy(&report.gps, &gps, sizeof(gps_coordinates_t));
    call ReportBuffer.enqueue(report);
    consume_report();
#endif
  }


#ifdef PLATFORM_TREMATEB 
  event void GpsNotify.notify(nmea_raw_t *val)
  {
    nmea_gga_msg_t last_lock;

    if (memcmp("$GPGGA", val->sentence, 6) != 0)
      return;

    if (call NmeaPacket.process(val, &last_lock) != SUCCESS ||
            last_lock.fixQuality == FIX_INVALID ||
            (last_lock.latitude.direction != 'N' &&
             last_lock.latitude.direction != 'S')) {
      call Leds.led1Toggle();
      return;
    }

    gps.latitude.degree = last_lock.latitude.degree;
    gps.latitude.minute = last_lock.latitude.minute;
    gps.latitude.direction = last_lock.latitude.direction;
    gps.longitude.degree = last_lock.longitude.degree;
    gps.longitude.minute = last_lock.longitude.minute;
    gps.longitude.direction = last_lock.longitude.direction;
    gps.time.hour = last_lock.time.hour;
    gps.time.min = last_lock.time.minute;
    gps.time.sec = last_lock.time.second;
    gps.timestamp = call LocalTime.get();

    call Leds.led1On();
  }


  task void reset_log()
  {
    uint16_t i;
    
    for (i = 0; i < 10; i++)
      fram_header.magic[i] = i;
    fram_header.entries = 0;

    fram_state = FRAM_RESET;
    if (call Fram.write(0, &fram_header, sizeof(struct fram_header)) != SUCCESS)
      post reset_log();
  }


  event void Fram.readDone(storage_addr_t addr, void *buf, storage_len_t len,
          error_t error)
  {
    uint16_t i;
    if (error != SUCCESS)
      return;
    for (i = 0; i < 10; i++)
      if (fram_header.magic[i] != i) {
        post reset_log();
        return;
      }
    call GpsControl.start();
    call AMControl.start();
  }


  void reset_done(storage_addr_t addr, void *buf, storage_len_t len,
          error_t error)
  {
    if (error != SUCCESS) {
      post reset_log();
      return;
    }
    call GpsControl.start();
    call AMControl.start();
  }

  
  void write_header_done(storage_addr_t addr, void *buf, storage_len_t len,
          error_t error)
  {
    if (error != SUCCESS)
      goto header_error;
    fram_state = FRAM_WRITE_LOG;
    addr = sizeof(struct fram_header) + 
        (fram_header.entries - 1) * sizeof(report_t);
    if (call Fram.write(addr, &report_buf, sizeof(report_t)) == SUCCESS)
      return;
header_error:
    fram_header.entries--;
    report_busy = FALSE;
  }
  
  
  void write_log_done(storage_addr_t addr, void *buf, storage_len_t len,
          error_t error)
  {
    report_busy = FALSE;
    if (error != SUCCESS) 
      fram_header.entries--;
    call Leds.led2Toggle();
    next_report();
  }

  event void Fram.writeDone(storage_addr_t addr, void *buf, storage_len_t len,
          error_t error)
  {
    switch (fram_state) {
      case FRAM_RESET:
        return reset_done(addr, buf, len, error);
      case FRAM_WRITE_HEADER:
        return write_header_done(addr, buf, len, error);
      case FRAM_WRITE_LOG:
        return write_log_done(addr, buf, len, error);
    }
  }


  event void GpsControl.startDone(error_t error)
  {
    if (error == SUCCESS)
      return;
    call GpsControl.start();
  }


  event void GpsControl.stopDone(error_t error)
  {
  }
#endif
}

