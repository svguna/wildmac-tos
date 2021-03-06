#ifndef __NEIGHBOR_DETECTION_H
#define __NEIGHBOR_DETECTION_H

enum {
  AM_CONFIG = 0x10,
  AM_EXPERIMENT_CTRL,
  AM_REPORT,
  AM_REPORT_CTRL
};


typedef nx_struct report_ctrl {
  nx_uint32_t delay;
  nx_uint16_t flush;
} report_ctrl_t;


typedef nx_struct experiment_ctrl {
  nx_uint32_t period;
  nx_uint16_t beacon;
  nx_uint16_t samples;
  nx_uint32_t timeout;
  nx_uint16_t delay;
  nx_uint8_t randomDelay;
  nx_uint8_t countFromMsgRcv;
  nx_uint8_t noRebroadcast;
  nx_uint32_t seed;
} experiment_ctrl_t;


typedef nx_struct report {
  nx_uint16_t src;
  nx_uint16_t addr;
  nx_uint32_t timestamp;
  nx_uint32_t absolute_timestamp;
  nx_uint32_t absolute_time;
  nx_uint32_t seq;
} report_t;

#endif
