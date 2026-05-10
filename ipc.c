/*
 * ipc.c
 * -----
 * Implementation of IPC helpers for SHM + semaphores.
 *
 * Semaphore explanation:
 *   ONE semaphore (a "set" of size 1) initialized to 1.
 *   - sem_lock()   = P operation (decrement). Blocks if locked.
 *   - sem_unlock() = V operation (increment). Releases.
 *
 * SEM_UNDO flag:
 *   If a process holding the lock crashes, kernel undoes the lock
 *   automatically. Without this, a crash deadlocks the system.
 */

#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ipc.h>
#include <sys/sem.h>
#include <sys/shm.h>


#include "config.h"
#include "ipc.h"

/* ---------- Module-level state ---------- */
static int g_shmid = -1;
static int g_semid = -1;
static SharedData *g_shm = NULL;

/* ---------- semun: required for semctl on Linux ----------
 * On Linux, this union is NOT defined in <sys/sem.h>.
 * If you get a redefinition error on your system, remove this block.
 */
union semun {
  int val;
  struct semid_ds *buf;
  unsigned short *array;
};

/* ---------- Helper: do a semaphore operation ---------- */
static int sem_op(int op) {
  struct sembuf sb;
  sb.sem_num = 0;
  sb.sem_op = op;
  sb.sem_flg = SEM_UNDO;

  if (semop(g_semid, &sb, 1) == -1) {
    if (errno != EINTR) {
      perror("semop");
    }
    return -1;
  }
  return 0;
}

/* ---------- Public API ---------- */

SharedData *ipc_init(void) {
  /* 1. Create SHM */
  g_shmid = shmget(SHM_KEY, sizeof(SharedData), IPC_CREAT | 0666);
  if (g_shmid == -1) {
    perror("[IPC] shmget failed");
    return NULL;
  }

  g_shm = (SharedData *)shmat(g_shmid, NULL, 0);
  if (g_shm == (void *)-1) {
    perror("[IPC] shmat failed");
    shmctl(g_shmid, IPC_RMID, NULL);
    return NULL;
  }

  /* 2. Create semaphore set with 1 semaphore */
  g_semid = semget(SEM_KEY, 1, IPC_CREAT | 0666);
  if (g_semid == -1) {
    perror("[IPC] semget failed");
    shmdt(g_shm);
    shmctl(g_shmid, IPC_RMID, NULL);
    return NULL;
  }

  /* 3. Initialize semaphore value to 1 (unlocked) */
  union semun arg;
  arg.val = 1;
  if (semctl(g_semid, 0, SETVAL, arg) == -1) {
    perror("[IPC] semctl SETVAL failed");
    shmdt(g_shm);
    shmctl(g_shmid, IPC_RMID, NULL);
    semctl(g_semid, 0, IPC_RMID);
    return NULL;
  }

  printf("[IPC] init OK. shmid=%d semid=%d\n", g_shmid, g_semid);
  return g_shm;
}

SharedData *ipc_attach(void) {
  /* Attach to EXISTING SHM (no IPC_CREAT) */
  g_shmid = shmget(SHM_KEY, sizeof(SharedData), 0666);
  if (g_shmid == -1) {
    perror("[IPC] shmget (attach) failed");
    return NULL;
  }

  g_shm = (SharedData *)shmat(g_shmid, NULL, 0);
  if (g_shm == (void *)-1) {
    perror("[IPC] shmat (attach) failed");
    return NULL;
  }

  /* Attach to existing semaphore */
  g_semid = semget(SEM_KEY, 1, 0666);
  if (g_semid == -1) {
    perror("[IPC] semget (attach) failed");
    shmdt(g_shm);
    return NULL;
  }

  return g_shm;
}

void ipc_detach(SharedData *shm) {
  if (shm != NULL) {
    shmdt(shm);
  }
  g_shm = NULL;
}

void ipc_destroy(void) {
  if (g_shm != NULL) {
    shmdt(g_shm);
    g_shm = NULL;
  }
  if (g_shmid != -1) {
    shmctl(g_shmid, IPC_RMID, NULL);
    g_shmid = -1;
  }
  if (g_semid != -1) {
    semctl(g_semid, 0, IPC_RMID);
    g_semid = -1;
  }
  printf("[IPC] destroyed.\n");
}

void sem_lock(void) { sem_op(-1); /* P */ }

void sem_unlock(void) { sem_op(+1); /* V */ }