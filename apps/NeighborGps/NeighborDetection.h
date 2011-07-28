#ifndef __NEIGHBOR_DETECTION_H
#define __NEIGHBOR_DETECTION_H

enum {
  AM_CONFIG = 0x10,
  AM_EXPERIMENT_CTRL,
  AM_REPORT,
  AM_REPORT_CTRL,
  AM_LOG_STATS
};


typedef nx_struct report_ctrl {
  nx_uint16_t flush;
} report_ctrl_t;


typedef nx_struct experiment_ctrl {
  nx_uint32_t period;
  nx_uint16_t beacon;
  nx_uint16_t samples;
  nx_uint32_t seed;
  nx_uint16_t power;
  nx_uint16_t node;
} experiment_ctrl_t;


typedef nx_struct coordinate {
  nx_uint8_t direction;
  nx_uint8_t degree;
  nx_uint16_t minute;
} coordinate_t;


typedef nx_struct timestamp {
  nx_uint8_t hour, min, sec;
} timestamp_t;


typedef nx_struct gps_coordinates {
  coordinate_t latitude;
  coordinate_t longitude;
  timestamp_t time;
  nx_uint32_t timestamp;
} gps_coordinates_t;


typedef nx_struct report {
  nx_uint16_t src;
  nx_uint16_t addr;
  nx_uint32_t timestamp;
  gps_coordinates_t gps;
} report_t;

typedef nx_struct log_stats {
  nx_uint32_t entries;
} log_stats_t;


struct fram_header {
  uint8_t magic[10];
  uint32_t entries;
};

#endif
