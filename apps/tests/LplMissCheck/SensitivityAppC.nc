configuration SensitivityAppC {}
implementation {
  components MainC, SensitivityC as App, LedsC;
  components ActiveMessageC;
  components new AMReceiverC(0x69);
  components new AMSenderC(0x69);
  components new TimerMilliC() as SendTimerC;

  App.Boot -> MainC.Boot;
  App.AMControl -> ActiveMessageC;
  App.Receive -> AMReceiverC;
  App.Packet -> ActiveMessageC;
  App.AMSend -> AMSenderC;
  App.SendTimer -> SendTimerC;
  
  App.Leds -> LedsC;
  App.LowPowerListening -> ActiveMessageC;
}

