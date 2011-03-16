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
    interface Timer<TMilli> as ReportTimer;
  }
}
implementation {
  message_t pkt;
  bool busy = FALSE;
  unsigned int sent_counter = 0;
  unsigned int receive_counter = 0;

  event void Boot.booted() 
  {
    call LowPowerListening.setLocalWakeupInterval(100);
    call AMControl.start();
  }


  event void AMControl.startDone(error_t err) {
    if (err != SUCCESS) {
      call AMControl.start();
      return;
    }

    if (TOS_NODE_ID == 2)
      call SendTimer.startPeriodic(500);

    call ReportTimer.startPeriodic(2000);
  }

  
  void report_receive()
  {
    printf("received %u packets\n", receive_counter);
    printfflush();
  }


  void report_send()
  {
    if (call SendTimer.isRunning())
      printf("sent %u packets\n", sent_counter);
    else {
      call Leds.led2On();
      printf("send completed!\n");
    }
    printfflush();
  }
  


  event void ReportTimer.fired()
  {
    switch (TOS_NODE_ID) {
      case 1: return report_receive();
      case 2: return report_send();
    }
  }


  event void AMControl.stopDone(error_t err) 
  {
  }


  event void SendTimer.fired()
  {
    unsigned int *seq;
    if (busy)
      return;

    seq = (unsigned int *) call Packet.getPayload(&pkt, 0);
    *seq = sent_counter;

    call LowPowerListening.setRemoteWakeupInterval(&pkt, 100); 
    if (call AMSend.send(AM_BROADCAST_ADDR, &pkt, 10) == SUCCESS)
      busy = TRUE;
  }


  event void AMSend.sendDone(message_t *msg, error_t error)
  {
    busy = FALSE;
    if (error == SUCCESS)
      sent_counter++;

    if (sent_counter == 10000)
      call SendTimer.stop();
  }


  event message_t * Receive.receive(message_t *msg, void *payload, uint8_t len)
  {
    receive_counter++;
    return msg;
  }
}

