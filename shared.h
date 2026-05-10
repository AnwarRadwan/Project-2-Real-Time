/*
 * shared.h
 * --------
 * Defines the SharedData struct that lives in shared memory.
 * Every process attaches to the same SHM and reads/writes this struct.
 */

#ifndef SHARED_H
#define SHARED_H

#include "config.h"
#include <time.h>


typedef struct {
  /* Traffic light state for each direction (RED/YELLOW/GREEN) */
  int light[NUM_DIRECTIONS];

  /* Number of vehicles waiting in each direction */
  int vehicle_count[NUM_DIRECTIONS];

  /* Pedestrian request: 1 if there's a pending request, 0 otherwise */
  int pedestrian_request;
  int pedestrian_direction; /* which direction the pedestrian wants to cross */

  /* Emergency mode: 1 if active, 0 otherwise */
  int emergency_mode;
  int emergency_direction; /* which direction has priority */

  /* Global shutdown flag — when set, all processes exit */
  int shutdown;

} SharedData;

#endif /* SHARED_H */