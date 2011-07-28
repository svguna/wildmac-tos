/**
 * Interface to allow application to make use of the neighbor detection service.
 * This service allows aggresive power saving on the radio when no other motes
 * are in the range.
 * 
 * @author Stefan Guna
 * @date   Aug 24, 2010
 */

interface NeighborDetection {
  /**
   * Starts the neighbor discovery service.
   * @param period The period with which hearbeats are sent and CCA samples are
   *               taken.
   * @param beacon The duration of a heartbeat. The heartbeat is sent at a
   *               random offset within a period.
   * @param samples The number of CCA samples to take immediately after the 
   *                beacon is sent. Two consecutive samples are separated by a
   *                beacon's duration.
   */
  command void start(uint32_t period, uint16_t beacon, uint16_t samples);


  /** Sets the power for the transmission of neighbor discovery beacons.
   * @param beaconPower The power level.
   */
  command void setBeaconPower(uint16_t beaconPower);
  
  /**
   * Stops the neighbor discovery service.
   */
  command void stop();
  
  /**
   * Signaled in response to the detection of a neighbor. Higher-level layers
   * are in charge of analysing false positivies. As soon as a neighbor is
   * detected, the service is stopped. It is the duty of higher-level layers
   * to determine when no neighbors are in range and to restart the service.
   */
  event void detected(am_addr_t addr, void *payload, uint8_t len);


  /** 
   * Signals the application to request a payload to pe appended to the beacon
   * messages. This is triggered for every outbound beacon.
   */
  event void * getPayload(uint8_t *len);
}

