#include "NeighborDetection.h"

configuration LogDownloadAppC {
}

implementation {
  components MainC, LogDownloadC as App, LedsC;
  
  components new SerialAMReceiverC(AM_REPORT_CTRL);
  components new SerialAMSenderC(AM_REPORT) as ReportSender;
  components new SerialAMSenderC(AM_LOG_STATS) as LogStatsSender; 
  components SerialActiveMessageC;
  components FramStorageC;
  components DcoCalibrationC;
  
  App.Boot -> MainC.Boot;
  
  App.Leds -> LedsC;
 
  App.Fram -> FramStorageC.PersistentStorage[unique("fram")];

  App.SerialReceive -> SerialAMReceiverC;
  App.SerialReport -> ReportSender;
  App.SerialLogStats -> LogStatsSender;
  App.SerialControl -> SerialActiveMessageC;
  App.SerialPacket -> SerialActiveMessageC;
}

