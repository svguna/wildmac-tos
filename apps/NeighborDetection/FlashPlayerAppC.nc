#include "StorageVolumes.h"
#include "NeighborDetection.h"

configuration FlashPlayerAppC {}
implementation {
  components MainC, FlashPlayerC as App, LedsC;
  components new SerialAMSenderC(AM_REPORT);
  components new SerialAMReceiverC(AM_REPORT_CTRL);
  components new TimerMilliC() as ReportDelay;
  components SerialActiveMessageC;

  components new LogStorageC(VOLUME_CONTACTLOG, TRUE);
  App.LogRead -> LogStorageC;
  App.LogWrite -> LogStorageC;

  App.Boot -> MainC.Boot;
  App.Leds -> LedsC;
  App.ReportDelay -> ReportDelay;
  
  App.SerialReceive -> SerialAMReceiverC;
  App.SerialSend -> SerialAMSenderC;
  App.SerialControl -> SerialActiveMessageC;
  App.SerialPacket -> SerialAMSenderC;
}

