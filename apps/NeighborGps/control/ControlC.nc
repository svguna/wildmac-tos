module ControlC {
  uses {
    interface Leds;
    interface Boot;
    
    interface Receive as SerialReceive;
    interface SplitControl as SerialControl;
    interface Packet as SerialPacket;
    
    interface SplitControl as AMControl;
    interface Packet as RadioPacket;
    interface AMSend;
  }
}

implementation {
  message_t radio_pkt;
  bool radio_busy = FALSE;
  experiment_ctrl_t experiment;
  
  event void Boot.booted() 
  {
    call SerialControl.start();
    call AMControl.start();
  }


  task void send_experiment()
  {
    error_t status;
    void *payload;

    if (radio_busy)
      return;

    payload = call RadioPacket.getPayload(&radio_pkt, 0);
    memcpy(payload, &experiment, sizeof(experiment_ctrl_t));

    status = call AMSend.send(experiment.node, &radio_pkt,
            sizeof(experiment_ctrl_t));

    if (status != SUCCESS) {
      post send_experiment();
      return;
    }
    radio_busy = TRUE;
  }


  event message_t *SerialReceive.receive(message_t *msg, void *payload,
          uint8_t len)
  {
    memcpy(&experiment, payload, len);
    post send_experiment();
    return msg;
  }

 
  event void AMSend.sendDone(message_t *msg, error_t error) 
  {
    radio_busy = FALSE;

    if (error != SUCCESS)
      post send_experiment();
    else
      call Leds.led2Toggle();

  }


  event void SerialControl.startDone(error_t err)
  {
    if (err == SUCCESS)
      return;
    call SerialControl.start();
  }


  event void SerialControl.stopDone(error_t err)
  {
  }


  event void AMControl.startDone(error_t err) {
    if (err == SUCCESS)
      return;
    call AMControl.start();
  }


  event void AMControl.stopDone(error_t err) 
  {
  }

 
}

