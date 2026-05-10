/*An example of using shared memory and semaphores for IPC in C.
 * main.c (Stage 2 — SHM + Semaphores)
 * ------------------------------------
 * Now uses ipc_init() and sem_lock/unlock() to safely write to SHM.
 *
 * Compile: gcc -Wall main.c ipc.c -o main
 * Run:     ./main
 */

#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>


#include "config.h"
#include "ipc.h"
#include "shared.h"


static SharedData *g_shm = NULL;

static void cleanup_and_exit(int sig) {
  (void)sig;
  printf("\n[MAIN] caught signal — cleaning up...\n");
  ipc_destroy();
  exit(0);
}

int main(void) {
  /* 1. Initialize IPC (SHM + semaphore) */
  g_shm = ipc_init();
  if (g_shm == NULL) {
    fprintf(stderr, "[MAIN] ipc_init failed.\n");
    return 1;
  }

  signal(SIGINT, cleanup_and_exit);
  signal(SIGTERM, cleanup_and_exit);

  /* 2. Initialize SHM contents — under lock */
  sem_lock();
  memset(g_shm, 0, sizeof(SharedData));
  g_shm->light[NORTH] = GREEN;
  g_shm->light[SOUTH] = GREEN;
  g_shm->light[EAST] = RED;
  g_shm->light[WEST] = RED;
  g_shm->vehicle_count[NORTH] = 3;
  g_shm->vehicle_count[SOUTH] = 1;
  g_shm->vehicle_count[EAST] = 0;
  g_shm->vehicle_count[WEST] = 2;
  sem_unlock();

  printf("[MAIN] SHM initialized.\n");
  printf("[MAIN] Run ./reader in another terminal. Ctrl+C to exit.\n");

  /* 3. Periodically modify SHM under lock */
  int counter = 0;
  while (1) {
    sleep(2);
    counter++;

    sem_lock();
    g_shm->vehicle_count[NORTH] = counter;
    if (counter % 5 == 0) {
      g_shm->light[EAST] = (g_shm->light[EAST] == RED) ? GREEN : RED;
    }
    sem_unlock();

    printf("[MAIN] tick %d  (wrote vehicle_count[N]=%d under lock)\n", counter,
           counter);
  }

  ipc_destroy();
  return 0;
}