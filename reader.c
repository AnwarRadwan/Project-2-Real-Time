/*
 * reader.c (Stage 2 test — uses ipc_attach + sem_lock)
 * ----------------------------------------------------
 * Compile: gcc -Wall reader.c ipc.c -o reader
 * Run:     ./reader     (after running ./main first)
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#include "config.h"
#include "ipc.h"
#include "shared.h"


static const char *light_str(int s) {
  switch (s) {
  case RED:
    return "RED";
  case YELLOW:
    return "YELLOW";
  case GREEN:
    return "GREEN";
  default:
    return "?";
  }
}

int main(void) {
  SharedData *shm = ipc_attach();
  if (shm == NULL) {
    fprintf(stderr, "[READER] ipc_attach failed (run ./main first?)\n");
    return 1;
  }
  printf("[READER] attached to SHM successfully.\n");

  for (int i = 0; i < 5; i++) {
    SharedData snap;

    /* Critical section: copy struct out under lock,
     * then print AFTER unlocking (to keep the lock short). */
    sem_lock();
    snap = *shm;
    sem_unlock();

    printf("\n[READER] read #%d:\n", i + 1);
    printf("  Lights:   N=%s  S=%s  E=%s  W=%s\n", light_str(snap.light[NORTH]),
           light_str(snap.light[SOUTH]), light_str(snap.light[EAST]),
           light_str(snap.light[WEST]));
    printf("  Vehicles: N=%d  S=%d  E=%d  W=%d\n", snap.vehicle_count[NORTH],
           snap.vehicle_count[SOUTH], snap.vehicle_count[EAST],
           snap.vehicle_count[WEST]);

    sleep(1);
  }

  ipc_detach(shm);
  printf("\n[READER] detached. Done.\n");
  return 0;
}