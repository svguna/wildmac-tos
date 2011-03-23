#include "printf.h"

module SensitivityC {
  uses {
    interface Leds;
    interface Boot;
    interface SplitControl as AMControl;
    interface Receive;
    interface Packet;
    interface AMSend;
    interface LowPowerListening;
    interface Timer<TMilli> as SendTimer;
  }
}
implementation {
  message_t pkt;
  unsigned int receive_counter = 0;

  event void Boot.booted() 
  {
    call LowPowerListening.setLocalWakeupInterval(100);
    call AMControl.start();
  }


  task void attemptSend()
  {
    unsigned int *seq;

    seq = (unsigned int *) call Packet.getPayload(&pkt, 0);
    *seq = 0;

    call LowPowerListening.setRemoteWakeupInterval(&pkt, 100); 
    if (call AMSend.send(AM_BROADCAST_ADDR, &pkt, 10) != SUCCESS) {
      post attemptSend();
      return;
    }

    call Leds.led2On();
    call SendTimer.startOneShot(3600000);
  }


  event void AMControl.startDone(error_t err) {
    if (err != SUCCESS) {
      call AMControl.start();
      return;
    }

    if (TOS_NODE_ID == 2) {
      post attemptSend();
      return;
    }
  }


  event void AMControl.stopDone(error_t err) 
  {
  }


  event void SendTimer.fired()
  {
    call Leds.led2Off();
  }


  event void AMSend.sendDone(message_t *msg, error_t error)
  {
    if (error == SUCCESS)
      return;
    call Leds.led2Off();
    post attemptSend();
  }


  event message_t * Receive.receive(message_t *msg, void *payload, uint8_t len)
  {
    call Leds.led1Toggle();
    return msg;
  }
}

