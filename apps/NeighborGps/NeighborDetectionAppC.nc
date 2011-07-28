#include "NeighborDetection.h"

configuration NeighborDetectionAppC {}
implementation {
  components MainC, NeighborDetectionC as App, LedsC;
  components new AMReceiverC(AM_CONFIG);
  components new TimerMilliC() as ExperimentDelayC;
  components ActiveMessageC;
  components new QueueC(report_t, 50) as ReportBufferC;
  components RandC;
  components HilTimerMilliC as Time;

#ifdef PLATFORM_TREMATEB
  components DcoCalibrationC;
  components FramStorageC;
  components Up501C;
  components NmeaGgaC;
#endif

  App.Boot -> MainC.Boot;
  
  App.Receive -> AMReceiverC;
  App.AMControl -> ActiveMessageC;
  App.Leds -> LedsC;
  App.ExperimentDelay -> ExperimentDelayC;
 
  App.NeighborDetection -> ActiveMessageC;
 
#ifdef PLATFORM_TREMATEB
  App.Fram -> FramStorageC.PersistentStorage[unique("fram")];
  App.ReportBuffer -> ReportBufferC;

  App.GpsControl -> Up501C;
  App.GpsNotify -> Up501C;
  App.NmeaPacket -> NmeaGgaC;
#endif

  App.Random -> RandC;
  App.SeedInit -> RandC;

  App.LocalTime -> Time;

  components RandomC;
  App.SystemSeedInit -> RandomC;
}

