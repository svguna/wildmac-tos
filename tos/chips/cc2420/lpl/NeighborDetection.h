#ifndef __NEIGHBOR_DETECTION
#define __NEIGHBOR_DETECTION

#ifndef DEFINED_TOS_NEIGHBOR_GROUP
#define DEFINED_TOS_NEIGHBOR_GROUP 0x23
#endif

#if (DEFINED_TOS_NEIGHBOR_GROUP == DEFINED_TOS_AM_GROUP)
#error "The DESTPAN for the neighbor detection service must be different from the DESTPAN of the AM stack"
#endif

#ifndef MAX_NEIGHBOR_CCA_CHECKS

#if defined(PLATFORM_TELOSB) || defined(PLATFORM_TMOTE)
#define MAX_NEIGHBOR_CCA_CHECKS 400
#else
#ifdef PLATFORM_TREMATEB
#warning "The value of MAX_NEIGHBOR_CCA_CHECKS must be validated."
#define MAX_NEIGHBOR_CCA_CHECKS 800
#else
#define MAX_NEIGHBOR_CCA_CHECKS 400
#endif
#endif

#endif

#ifndef MIN_NEIGHBOR_SAMPLES
#define MIN_NEIGHBOR_SAMPLES 0
#endif

#ifndef NEIGHBOR_ON_TIME
#define NEIGHBOR_ON_TIME 14
#endif

#ifndef NEIGHBOR_DELAY_AFTER_RCV
#define NEIGHBOR_DELAY_AFTER_RCV 10
#endif

enum {
  TOS_NEIGHBOR_GROUP = DEFINED_TOS_NEIGHBOR_GROUP
};

#endif

