#ifndef __NEIGHBOR_DETECTION
#define __NEIGHBOR_DETECTION

#ifndef DEFINED_TOS_NEIGHBOR_GROUP
#define DEFINED_TOS_NEIGHBOR_GROUP 0x23
#endif

#if (DEFINED_TOS_NEIGHBOR_GROUP == DEFINED_TOS_AM_GROUP)
#error "The DESTPAN for the neighbor detection service must be different from the DESTPAN of the AM stack"
#endif

enum {
  TOS_NEIGHBOR_GROUP = DEFINED_TOS_NEIGHBOR_GROUP
};

#endif

