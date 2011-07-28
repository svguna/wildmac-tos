module LogDownloadC {
  uses {
    interface Leds;
    interface Boot;
   
    interface PersistentStorage as Fram;

    interface Receive as SerialReceive;
    interface SplitControl as SerialControl;
    interface Packet as SerialPacket;
    interface AMSend as SerialReport;
    interface AMSend as SerialLogStats;
  }
}

implementation {
  message_t serial_pkt;
  struct fram_header fram_header;
  report_t report;
  bool busy = FALSE;
  uint32_t progress;
  bool erase = FALSE;
  
  event void Boot.booted() 
  {
    call SerialControl.start();
  }


  event message_t *SerialReceive.receive(message_t *msg, void *payload,
          uint8_t len)
  {
    report_ctrl_t *report_ctrl = (report_ctrl_t *) payload;
    call Leds.led1On();
    if (busy)
      return msg;

    progress = 0;

    if (len != sizeof(report_ctrl_t)) {
      call Leds.led0On();
      return msg;
    }
   
    busy = TRUE;
    erase = report_ctrl->flush;
 
    call Leds.led0Off();
    call Leds.led1On();
    
    if (call Fram.read(0, &fram_header, sizeof(struct fram_header)) != SUCCESS)
      call Leds.led0On();
    
    return msg;
  }


  void reset_log()
  {
    uint16_t i;
    for (i = 0; i < 10; i++)
      fram_header.magic[i] = i;
    fram_header.entries = 0;

    if (call Fram.write(0, &fram_header, sizeof(struct fram_header)) != SUCCESS)
      call Leds.led0On();
  }

  
  void cleanup()
  {
    busy = FALSE;
    call Leds.led1Off();
    call Leds.led2Off();
  }


  void complete_report()
  {
    if (!erase)
      cleanup();
    else
      reset_log();
  }


  void read_next_report()
  {
    if (fram_header.entries == progress) {
      complete_report();
      return;
    }
    if (call Fram.read(sizeof(struct fram_header) + sizeof(report_t) * progress,
                &report, sizeof(report_t)) != SUCCESS)
      call Leds.led0On();
  }

 
  event void SerialReport.sendDone(message_t *msg, error_t error) 
  {
    if (error != SUCCESS) {
      call Leds.led0On();
      return;
    }
    call Leds.led2Toggle();
    progress++;
    read_next_report();
  }


  event void SerialLogStats.sendDone(message_t *msg, error_t error) 
  {
    if (error != SUCCESS) {
      call Leds.led0On();
      return;
    }
    call Leds.led2Toggle();
    read_next_report();
  }

  
  void process_header()
  {
    uint16_t i;
    log_stats_t *log_stats;

    for (i = 0; i < 10; i++)
      if (fram_header.magic[i] != i) {
        call Leds.led0On();
        return;
      }
   
    log_stats = (log_stats_t *) call SerialPacket.getPayload(&serial_pkt, 0);
    log_stats->entries = fram_header.entries;
    
    if (call SerialLogStats.send(AM_BROADCAST_ADDR, &serial_pkt, 
                sizeof(log_stats_t)) != SUCCESS)
      call Leds.led0On();
  }


  void process_report()
  {
    uint16_t i;

    for (i = 0; i < 10; i++)
      if (fram_header.magic[i] != i) {
        call Leds.led0On();
        return;
      }
   
    memcpy(call SerialPacket.getPayload(&serial_pkt, 0), &report, 
            sizeof(report));
    
    if (call SerialReport.send(AM_BROADCAST_ADDR, &serial_pkt, sizeof(report_t))
                != SUCCESS)
      call Leds.led0On();
  }



  event void Fram.readDone(storage_addr_t addr, void *buf, storage_len_t len,
          error_t error)
  {
    if (error != SUCCESS) {
      call Leds.led0On();
      return;
    }

    if (buf == &fram_header)
      process_header();
    if (buf == &report)
      process_report();
  }

  
  event void Fram.writeDone(storage_addr_t addr, void *buf, storage_len_t len,
          error_t error)
  {
    if (error != SUCCESS) {
      call Leds.led0On();
      return;
    }
    cleanup();
  }


  event void SerialControl.startDone(error_t err)
  {
    if (err == SUCCESS) {
      call Leds.led2On();
      return;
    }
    call SerialControl.start();
  }


  event void SerialControl.stopDone(error_t err)
  {
  }
}

