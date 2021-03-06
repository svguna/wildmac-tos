/*
 * Copyright (c) 2005-2006 Rincon Research Corporation
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the
 *   distribution.
 * - Neither the name of the Rincon Research Corporation nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE
 * RINCON RESEARCH OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE
 */

/**
 * Low Power Listening for the CC2420.  This component is responsible for
 * delivery of an LPL packet, and for turning off the radio when the radio
 * has run out of tasks.
 *
 * The PowerCycle component is responsible for duty cycling the radio
 * and performing receive detections.
 *
 * @author David Moss
 */

#include "Lpl.h"
#include "DefaultLpl.h"
#include "AM.h"
#include "NeighborDetection.h"

module DefaultLplP {
  provides {
    interface Init;
    interface LowPowerListening;
    interface NeighborDetection;
    interface Send;
    interface Receive;
  }
  
  uses {
    interface Send as SubSend;
    interface CC2420Transmit as Resend;
    interface RadioBackoff;
    interface Receive as SubReceive;
    interface SplitControl as SubControl;
    interface PowerCycle;
    interface CC2420PacketBody;
    interface CC2420Packet;
    interface PacketAcknowledgements;
    interface State as SendState;
    interface State as RadioPowerState;
    interface State as SplitControlState;
    interface Timer<TMilli> as OffTimer;
    interface Timer<TMilli> as SendDoneTimer;
#ifdef NEIGHBOR_DETECTION
    interface Timer<TMilli> as NeighborTimer;
    interface Timer<TMilli> as HeartbeatTimer;
#endif
    interface Random;
    interface Leds;
    interface SystemLowPowerListening;
  }
}

implementation {
  
  /** The message currently being sent */
  norace message_t *currentSendMsg;
  
  /** The length of the current send message */
  uint8_t currentSendLen;
  
  /** TRUE if the radio is duty cycling and not always on */
  bool dutyCycling;

  norace bool inContact = TRUE;
  norace bool deterministic = FALSE;
  uint16_t beaconInterval = LPL_DEF_LOCAL_WAKEUP;

#ifdef NEIGHBOR_DETECTION
  message_t heartbeatMsg;
  uint32_t beaconDomain;
  uint16_t beaconPower;
  void neighbor_periodic();
#endif

  /**
   * Radio Power State
   */
  enum {
    S_OFF, // off by default
    S_TURNING_ON,
    S_ON,
    S_TURNING_OFF,
  };
  
  /**
   * Send States
   */
  enum {
    S_IDLE,
    S_SENDING,
  };
  
  enum {
    ONE_MESSAGE = 0,
  };
  
  /***************** Prototypes ***************/
  task void send();
  task void resend();
  task void startRadio();
  task void stopRadio();
  
  void initializeSend();
  void startOffTimer();
 
 
  void stop_neighbor_detection()
  {
#ifdef NEIGHBOR_DETECTION
    inContact = TRUE;

    call PowerCycle.setInContact();
    call HeartbeatTimer.stop();
    call NeighborTimer.stop();
#endif
  }


  /***************** Init Commands ***************/
  command error_t Init.init() {
    dutyCycling = FALSE;
    return SUCCESS;
  }
  
  /***************** LowPowerListening Commands ***************/
  /**
   * Set this this node's radio wakeup interval, in milliseconds.
   * Once every interval, the node will sleep and perform an Rx check 
   * on the radio.  Setting the wakeup interval to 0 will keep the radio
   * always on.
   *
   * @param intervalMs the length of this node's wakeup interval, in [ms]
   */
  command void LowPowerListening.setLocalWakeupInterval(uint16_t intervalMs) {
    call PowerCycle.setSleepInterval(intervalMs);
  }
  
  /**
   * @return the local node's wakeup interval, in [ms]
   */
  command uint16_t LowPowerListening.getLocalWakeupInterval() {
    return call PowerCycle.getSleepInterval();
  }
  
  /**
   * Configure this outgoing message so it can be transmitted to a neighbor mote
   * with the specified wakeup interval.
   * @param msg Pointer to the message that will be sent
   * @param intervalMs The receiving node's wakeup interval, in [ms]
   */
  command void LowPowerListening.setRemoteWakeupInterval(message_t *msg, 
      uint16_t intervalMs) {
    (call CC2420PacketBody.getMetadata(msg))->rxInterval = intervalMs;
  }
  
  /**
   * @return the destination node's wakeup interval configured in this message
   */
  command uint16_t LowPowerListening.getRemoteWakeupInterval(message_t *msg) {
    return (call CC2420PacketBody.getMetadata(msg))->rxInterval;
  }
  
  /***************** Send Commands ***************/
  /**
   * Each call to this send command gives the message a single
   * DSN that does not change for every copy of the message
   * sent out.  For messages that are not acknowledged, such as
   * a broadcast address message, the receiving end does not
   * signal receive() more than once for that message.
   */
  command error_t Send.send(message_t *msg, uint8_t len) {
    if(call SplitControlState.getState() == S_OFF) {
      // Everything is off right now, start SplitControl and try again
      return EOFF;
    }

    if (inContact == FALSE)
      return EBUSY;
    
    if(call SendState.requestState(S_LPL_SENDING) == SUCCESS) {
      currentSendMsg = msg;
      currentSendLen = len;
      
      // In case our off timer is running...
      call OffTimer.stop();
      call SendDoneTimer.stop();
      
      if(call RadioPowerState.getState() == S_ON) {
        initializeSend();
        return SUCCESS;
        
      } else {
        post startRadio();
      }
      
      return SUCCESS;
    }
    
    return EBUSY;
  }

  command error_t Send.cancel(message_t *msg) {
    if(currentSendMsg == msg) {
      call SendState.toIdle();
      call SendDoneTimer.stop();
      startOffTimer();
      return call SubSend.cancel(msg);
    }
    
    return FAIL;
  }
  
  
  command uint8_t Send.maxPayloadLength() {
    return call SubSend.maxPayloadLength();
  }

  command void *Send.getPayload(message_t* msg, uint8_t len) {
    return call SubSend.getPayload(msg, len);
  }
  
  
  /***************** RadioBackoff Events ****************/
  async event void RadioBackoff.requestInitialBackoff(message_t *msg) {
    if (inContact == FALSE) {
      call RadioBackoff.setInitialBackoff(0);
      return;
    }

    if((call CC2420PacketBody.getMetadata(msg))->rxInterval 
        > ONE_MESSAGE) {
      call RadioBackoff.setInitialBackoff( call Random.rand16() 
          % (0x4 * CC2420_BACKOFF_PERIOD) + CC2420_MIN_BACKOFF);
    }
  }
  
  async event void RadioBackoff.requestCongestionBackoff(message_t *msg) {
    if((call CC2420PacketBody.getMetadata(msg))->rxInterval 
        > ONE_MESSAGE) {
      call RadioBackoff.setCongestionBackoff( call Random.rand16() 
          % (0x3 * CC2420_BACKOFF_PERIOD) + CC2420_MIN_BACKOFF);
    }
  }
  
  async event void RadioBackoff.requestCca(message_t *msg) {
    if (inContact == FALSE && deterministic == FALSE) {
      call RadioBackoff.setCca(FALSE);
      return;
    }
  }
  

  /***************** DutyCycle Events ***************/
  /**
   * A transmitter was detected.  You must now take action to
   * turn the radio off when the transaction is complete.
   */
  event void PowerCycle.detected() {
    // At this point, the duty cycling has been disabled temporary
    // and it will be this component's job to turn the radio back off
    // Wait long enough to see if we actually receive a packet, which is
    // just a little longer in case there is more than one lpl transmitter on
    // the channel.
    startOffTimer();
  }
  
  
  /***************** SubControl Events ***************/
  event void SubControl.startDone(error_t error) {
#ifdef WILDMAC_DEMO
    if (error == SUCCESS) 
      call Leds.led0On();
#endif
    if(!error) {
      call RadioPowerState.forceState(S_ON);
      
      if(call SendState.getState() == S_LPL_FIRST_MESSAGE
          || call SendState.getState() == S_LPL_SENDING) {
        initializeSend();
      }
    }
  }
    
  event void SubControl.stopDone(error_t error) {
#ifdef WILDMAC_DEMO
    if (error == SUCCESS) 
      call Leds.led0Off();
#endif

    if(!error) {

      if(call SendState.getState() == S_LPL_FIRST_MESSAGE
          || call SendState.getState() == S_LPL_SENDING) {
        // We're in the middle of sending a message; start the radio back up
        post startRadio();
        
      } else {        
        call OffTimer.stop();
        call SendDoneTimer.stop();
      }
    }
  }
  
  /***************** SubSend Events ***************/
  event void SubSend.sendDone(message_t* msg, error_t error) {
   
    switch(call SendState.getState()) {
    case S_LPL_SENDING:
      if(call SendDoneTimer.isRunning()) {
        if(!call PacketAcknowledgements.wasAcked(msg)) {
          post resend();
          return;
        }
      }
      break;
      
    case S_LPL_CLEAN_UP:
      /**
       * We include this state so upper layers can't send a different message
       * before the last message gets done sending
       */
      break;
      
    default:
      break;
    }  
    
    call SendState.toIdle();
    call SendDoneTimer.stop();
    startOffTimer();
    if (inContact == TRUE) 
      signal Send.sendDone(msg, error);
    else
      call PowerCycle.startNeighborDetection();
  }
  
  /***************** SubReceive Events ***************/
  /**
   * If the received message is new, we signal the receive event and
   * start the off timer.  If the last message we received had the same
   * DSN as this message, then the chances are pretty good
   * that this message should be ignored, especially if the destination address
   * as the broadcast address
   */
  event message_t *SubReceive.receive(message_t* msg, void* payload, 
      uint8_t len) {
    cc2420_header_t *header = call CC2420PacketBody.getHeader(msg);

#ifndef NEIGHBOR_NO_STOP
    stop_neighbor_detection();
#endif
#ifdef NEIGHBOR_DETECTION
    if (header->destpan != TOS_NEIGHBOR_GROUP) 
      signal NeighborDetection.detected(header->src, NULL, 0);
    else
      signal NeighborDetection.detected(header->src, payload, len);
#endif

    startOffTimer();

    if (header->destpan == TOS_NEIGHBOR_GROUP) 
      return msg;
    return signal Receive.receive(msg, payload, len);
  }
  
  /***************** Timer Events ****************/
  event void OffTimer.fired() {    
    /*
     * Only stop the radio if the radio is supposed to be off permanently
     * or if the duty cycle is on and our sleep interval is not 0
     */
    if(call SplitControlState.getState() == S_OFF
        || inContact == FALSE
        || (call PowerCycle.getSleepInterval() > 0
            && call SplitControlState.getState() != S_OFF
                && call SendState.getState() == S_LPL_NOT_SENDING)) { 
      post stopRadio();
    }
  }
  
  /**
   * When this timer is running, that means we're sending repeating messages
   * to a node that is receive check duty cycling.
   */
  event void SendDoneTimer.fired() {
    if(call SendState.getState() == S_LPL_SENDING) {
      // The next time SubSend.sendDone is signaled, send is complete.
      call SendState.forceState(S_LPL_CLEAN_UP);
    }
  }
  
  /***************** Resend Events ****************/
  /**
   * Signal that a message has been sent
   *
   * @param p_msg message to send.
   * @param error notifaction of how the operation went.
   */
  async event void Resend.sendDone( message_t* p_msg, error_t error ) {
    // This is actually caught by SubSend.sendDone
  }
  
  
  /***************** Tasks ***************/
  task void send() {
    if(call SubSend.send(currentSendMsg, currentSendLen) != SUCCESS) {
      post send();
    } 
  }
  
  task void resend() {
    bool cca = TRUE;
    if (inContact == FALSE)
      cca = FALSE;

    if(call Resend.resend(cca) != SUCCESS) {
      post resend();
    }
  }
  
  task void startRadio() {
    if(call SubControl.start() != SUCCESS) {
      post startRadio();
    }
  }
  
  task void stopRadio() {
    if(call SendState.getState() == S_LPL_NOT_SENDING) {
      if(call SubControl.stop() != SUCCESS) {
        post stopRadio();
      }
    }
  }
  
  /***************** Functions ***************/
  void initializeSend() {
    uint16_t preamble = 
        call LowPowerListening.getRemoteWakeupInterval(currentSendMsg) + 20;
    
    if (inContact == FALSE)
      preamble = beaconInterval;
    
    if (preamble > ONE_MESSAGE) {
    
      if(inContact == FALSE ||
          (call CC2420PacketBody.getHeader(currentSendMsg))->dest == IEEE154_BROADCAST_ADDR) {
        call PacketAcknowledgements.noAck(currentSendMsg);
      } else {
        // Send it repetitively within our transmit window
        call PacketAcknowledgements.requestAck(currentSendMsg);
      }

      call SendDoneTimer.startOneShot(preamble);
    }
    post send();
  }
  
  
  void startOffTimer() {
#ifndef NO_DUTY_CYCLE
    uint32_t off_timeout = call SystemLowPowerListening.getDelayAfterReceive();

    if (inContact == FALSE)
      off_timeout = NEIGHBOR_DELAY_AFTER_RCV;

    call OffTimer.startOneShot(off_timeout);
#endif
  }


  command void NeighborDetection.setBeaconPower(uint16_t power)
  {
#ifdef NEIGHBOR_DETECTION
    beaconPower = power;
#endif
  }


  command void NeighborDetection.start(uint32_t period, uint16_t beacon,
          uint16_t samples)
  {
#ifdef NEIGHBOR_DETECTION
    deterministic = FALSE;
    if (beacon * (samples + 1) >= period / 2)
      deterministic = TRUE;

    inContact = FALSE;
    beaconInterval = beacon;
    beaconDomain = period - beacon * (samples + 1);
    
    call PowerCycle.setNeighborConfig(beacon, samples);
    call NeighborTimer.startPeriodic(period);
    neighbor_periodic();
#endif
  }


  command void NeighborDetection.stop()
  {
    stop_neighbor_detection();
  }

  
#ifdef NEIGHBOR_DETECTION
  uint8_t fill_heartbeatMsg(uint8_t len, void *payload)
  {
    cc2420_header_t *header = call CC2420PacketBody.getHeader(&heartbeatMsg);
      
    header->dsn++;
    header->destpan = TOS_NEIGHBOR_GROUP;
    header->dest = IEEE154_BROADCAST_ADDR;
    header->src = TOS_NODE_ID;
    header->network = TINYOS_6LOWPAN_NETWORK_ID;

    call CC2420Packet.setPower(&heartbeatMsg, beaconPower); 

    if (len > 0 && len < call SubSend.maxPayloadLength() && payload != NULL) 
      memcpy(call SubSend.getPayload(&heartbeatMsg, 0), payload, len);
    else
      len = 0;
    return len;
  }

  void broadcast_hearbeat()
  {
    uint8_t len;
    void *payload;

    payload = signal NeighborDetection.getPayload(&len);
    len = fill_heartbeatMsg(len, payload);

    currentSendMsg = &heartbeatMsg;
    currentSendLen = len;
    
    call OffTimer.stop();
    call SendDoneTimer.stop();
    
    if (call RadioPowerState.getState() == S_ON) 
      initializeSend();
    else 
      post startRadio();
  }


  void neighbor_periodic()
  {
    if (call SendState.requestState(S_LPL_SENDING) != SUCCESS)
      return;
    if (deterministic) {
      broadcast_hearbeat();
      return;
    }
    call HeartbeatTimer.startOneShot(call Random.rand16() % beaconDomain);
  }


  event void HeartbeatTimer.fired()
  {
    broadcast_hearbeat();
  }


  event void NeighborTimer.fired()
  {
    neighbor_periodic();
  }
#endif
}

