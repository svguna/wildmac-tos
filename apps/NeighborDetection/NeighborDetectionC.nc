// $Id$

/*									tab:4
 * "Copyright (c) 2000-2005 The Regents of the University  of California.  
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 * 
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
 * CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS."
 *
 * Copyright (c) 2002-2003 Intel Corporation
 * All rights reserved.
 *
 * This file is distributed under the terms in the attached INTEL-LICENSE     
 * file. If you do not find these files, copies can be found by writing to
 * Intel Research Berkeley, 2150 Shattuck Avenue, Suite 1300, Berkeley, CA, 
 * 94704.  Attention:  Intel License Inquiry.
 */
 
#include "Timer.h"
#include "NeighborDetection.h"

/**
 * Implementation of the RadioCountToLeds application. RadioCountToLeds 
 * maintains a 4Hz counter, broadcasting its value in an AM packet 
 * every time it gets updated. A RadioCountToLeds node that hears a counter 
 * displays the bottom three bits on its LEDs. This application is a useful 
 * test to show that basic AM communication and timers work.
 *
 * @author Philip Levis
 * @date   June 6 2005
 */

module NeighborDetectionC @safe() {
  uses {
    interface Leds;
    interface Boot;
    interface Receive;
    interface AMSend;
    interface Timer<TMilli> as ExperimentTimeout;
    interface Timer<TMilli> as ExperimentDelay;
    interface SplitControl as AMControl;
    interface Packet;
    
    interface NeighborDetection;
    interface LowPowerListening;

    interface Queue<am_addr_t> as DetectedNeighbors;
    interface Queue<report_t> as ReportBuffer;

    interface Receive as SerialReceive;
    interface AMSend as SerialSend;
    interface SplitControl as SerialControl;
    
    interface HplMsp430GeneralIO as UsbConnection;

    interface Random;
  }
}
implementation {

  uint32_t experiment_start;
  message_t serial_pkt, radio_pkt;
  bool serial_busy = FALSE, radio_busy = FALSE;
  bool run_experiment = FALSE;
  experiment_ctrl_t experiment;
  
  task void send_report()
  {
    report_t *report;
    error_t status;

    if (serial_busy || call ReportBuffer.empty())
      return;

    report = (report_t *) call Packet.getPayload(&serial_pkt, 0);
    *report = call ReportBuffer.head();

    status = call SerialSend.send(AM_BROADCAST_ADDR, &serial_pkt, 
            sizeof(report_t));

    if (status != SUCCESS) {
      post send_report();
      return;
    }
    serial_busy = TRUE;
  }


  task void send_experiment()
  {
    error_t status;
    void *payload;

    if (radio_busy)
      return;

    payload = call Packet.getPayload(&radio_pkt, 0);
    memcpy(payload, &experiment, sizeof(experiment_ctrl_t));

    status = call AMSend.send(AM_BROADCAST_ADDR, &radio_pkt,
            sizeof(experiment_ctrl_t));

    if (status != SUCCESS) {
      post send_experiment();
      return;
    }
    radio_busy = TRUE;
  }


  bool receive_message(message_t *msg, void *payload, uint8_t len)
  {
    uint16_t experiment_delay;

    if (len != sizeof(experiment_ctrl_t)) 
      return FAIL;
    
    while (!call DetectedNeighbors.empty())
      call DetectedNeighbors.dequeue();

    memcpy(&experiment, payload, len);
    
    if (experiment.randomDelay)
      experiment_delay = call Random.rand16() % experiment.delay;
    else
      experiment_delay = experiment.delay;
    
    call ExperimentDelay.startOneShot(experiment_delay);

    if (experiment.countFromMsgRcv)
      experiment_start = call ExperimentTimeout.getNow(); 
    
    return SUCCESS;
  }


  event void Boot.booted() 
  {
    call UsbConnection.selectIOFunc();
    call UsbConnection.makeInput();
    call SerialControl.start();
    call AMControl.start();
  }


  event void AMControl.startDone(error_t err) {
    if (err != SUCCESS) {
      call AMControl.start();
      return;
    }

    if (!run_experiment)
      return;

    call NeighborDetection.start(experiment.period, experiment.beacon,
            experiment.samples);
    call ExperimentTimeout.startOneShot(experiment.timeout);
    if (!experiment.countFromMsgRcv)
      experiment_start = call ExperimentTimeout.getNow(); 
    call Leds.led1On();
  }


  event void AMControl.stopDone(error_t err) 
  {
  }

  
  event void ExperimentDelay.fired()
  {
    run_experiment = TRUE;
    call AMControl.start();
  }


  event void ExperimentTimeout.fired()
  {
    call Leds.set(0xff);
    WDTCTL = 0;
  }

  
  event message_t *Receive.receive(message_t *msg, void *payload, uint8_t len) 
  {
    receive_message(msg, payload, len);
    call AMControl.stop();
    return msg;
  }


  event void AMSend.sendDone(message_t *msg, error_t error) 
  {
    radio_busy = FALSE;

    if (error != SUCCESS)
      post send_experiment();
    else
      call AMControl.stop();
  }


  event void NeighborDetection.detected(am_addr_t addr)
  {
    uint16_t i;
    report_t report;

    if (!call ExperimentTimeout.isRunning())
      return;

    call Leds.led2Toggle();
    for (i = 0; i < call DetectedNeighbors.size(); i++)
      if (call DetectedNeighbors.element(i) == addr)
        return;

    if (!call UsbConnection.get())
      return;

    report.addr = addr;
    report.timestamp = call ExperimentTimeout.getNow() - experiment_start;
    call ReportBuffer.enqueue(report);
    call DetectedNeighbors.enqueue(addr);
    post send_report(); 
  }


  event message_t *SerialReceive.receive(message_t *msg, void *payload,
          uint8_t len)
  {
    if (receive_message(msg, payload, len) == SUCCESS)
      post send_experiment();
    return msg;
  }

  
  event void SerialSend.sendDone(message_t *msg, error_t error) 
  {
    serial_busy = FALSE;

    if (error == SUCCESS)
      call ReportBuffer.dequeue();

    if (!call ReportBuffer.empty())
      post send_report(); 
  }


  event void SerialControl.startDone(error_t err)
  {
    if (err != SUCCESS)
      call SerialControl.start();
  }


  event void SerialControl.stopDone(error_t err)
  {
  }
}

