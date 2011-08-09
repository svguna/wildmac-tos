#include "StorageVolumes.h"
#include "NeighborDetection.h"

configuration NeighborDetectionAppC {}
implementation {
  components MainC, NeighborDetectionC as App, LedsC;
  components new AMSenderC(AM_CONFIG);
  components new AMReceiverC(AM_CONFIG);
  components new SerialAMSenderC(AM_REPORT);
  components new SerialAMReceiverC(AM_EXPERIMENT_CTRL);
  components new TimerMilliC() as ExperimentTimeoutC;
  components new TimerMilliC() as ExperimentDelayC;
  components ActiveMessageC;
  components HplMsp430GeneralIOC;
#ifndef DUPLICATE_REPORTS
  components new QueueC(am_addr_t, 20) as DetectedNeighborsC;
#endif
  components new QueueC(report_t, 50) as ReportBufferC;
  components SerialActiveMessageC;
  components RandC;
  components HilTimerMilliC as Time;

#ifdef FLASH_LOG
  components new LogStorageC(VOLUME_CONTACTLOG, TRUE);
  App.LogWrite -> LogStorageC;
#endif

  App.Boot -> MainC.Boot;
  
  App.Receive -> AMReceiverC;
  App.AMSend -> AMSenderC;
  App.AMControl -> ActiveMessageC;
  App.Leds -> LedsC;
  App.ExperimentTimeout -> ExperimentTimeoutC;
  App.ExperimentDelay -> ExperimentDelayC;
  App.RadioPacket -> AMSenderC;
  
  App.LowPowerListening -> ActiveMessageC;
  App.NeighborDetection -> ActiveMessageC;
  
  App.ReportBuffer -> ReportBufferC;
#ifndef DUPLICATE_REPORTS
  App.DetectedNeighbors -> DetectedNeighborsC;
#endif

  App.SerialReceive -> SerialAMReceiverC;
  App.SerialSend -> SerialAMSenderC;
  App.SerialControl -> SerialActiveMessageC;
  App.SerialPacket -> SerialAMSenderC;

  App.UsbConnection -> HplMsp430GeneralIOC.Port12;
  App.Random -> RandC;
  App.SeedInit -> RandC;

  App.LocalTime -> Time;

  components RandomC;
  App.SystemSeedInit -> RandomC;

#ifdef TIMESTAMP_BUTTON
  components UserButtonC;
  App.Button -> UserButtonC;
#endif
}

