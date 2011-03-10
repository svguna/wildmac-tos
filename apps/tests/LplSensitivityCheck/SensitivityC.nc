module SensitivityC {
  uses {
    interface Leds;
    interface Boot;
    interface SplitControl as AMControl;
    interface LowPowerListening;
  }
}
implementation {

  event void Boot.booted() 
  {
    call LowPowerListening.setLocalWakeupInterval(200);
    call AMControl.start();
  }


  event void AMControl.startDone(error_t err) {
    if (err != SUCCESS) {
      call AMControl.start();
      return;
    }
  }


  event void AMControl.stopDone(error_t err) 
  {
  }
}

