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
 * Dummy low power listening interface used when LowPowerListening is not
 * compiled in with the application.
 * Wakeup interval is always 0 (always on)
 * @author David Moss
 */
 
module DummyLplP {
  provides {
    interface LowPowerListening;
    interface NeighborDetection;
  }
}

implementation {

  command void LowPowerListening.setLocalWakeupInterval(uint16_t intervalMs) {
  }
  
  command uint16_t LowPowerListening.getLocalWakeupInterval() {
    return 0;
  }
  
  command void LowPowerListening.setRemoteWakeupInterval(message_t *msg, uint16_t intervalMs) {
  }
  
  command uint16_t LowPowerListening.getRemoteWakeupInterval(message_t *msg) {
    return 0;
  }
  
  command void NeighborDetection.setBeaconPower(uint16_t power)
  {
  }

  command void NeighborDetection.start(uint32_t period, uint16_t beacon,
          uint16_t samples)
  {
  }


  command void NeighborDetection.stop()
  {
  }
}

