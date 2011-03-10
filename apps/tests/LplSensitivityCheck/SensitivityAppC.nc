configuration SensitivityAppC {}
implementation {
  components MainC, SensitivityC as App, LedsC;
  components ActiveMessageC;

  App.Boot -> MainC.Boot;
  App.AMControl -> ActiveMessageC;
  
  App.Leds -> LedsC;
  App.LowPowerListening -> ActiveMessageC;
}

