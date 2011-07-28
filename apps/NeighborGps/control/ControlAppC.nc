#include "NeighborDetection.h"

configuration ControlAppC {
}

implementation {
  components MainC, ControlC as App, LedsC;
  components new AMSenderC(AM_CONFIG);
  components new SerialAMReceiverC(AM_EXPERIMENT_CTRL);
  components ActiveMessageC;
  components SerialActiveMessageC;
  
  App.Boot -> MainC.Boot;
  
  App.AMSend -> AMSenderC;
  App.AMControl -> ActiveMessageC;
  App.Leds -> LedsC;
  App.RadioPacket -> AMSenderC;
 
  App.SerialReceive -> SerialAMReceiverC;
  App.SerialControl -> SerialActiveMessageC;
  App.SerialPacket -> SerialActiveMessageC;

}

