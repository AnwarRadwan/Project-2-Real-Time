/*
 * ipc.h
 * -----
 * IPC helper functions: SHM creation/attachment + semaphore lock/unlock.
 * Stage 2: Shared Memory + Semaphores.
 */

#ifndef IPC_H
#define IPC_H

#include "shared.h"

/* ---------- SHM lifecycle ---------- */

/* Create or open the SHM segment AND the semaphore.
 * Call this ONCE from main.c (the parent process).
 * Returns pointer to attached SharedData, or NULL on error.
 */
SharedData *ipc_init(void);

/* Attach to an EXISTING SHM segment (created by main).
 * Call this from child processes.
 * Returns pointer to attached SharedData, or NULL on error.
 */
SharedData *ipc_attach(void);

/* Detach from SHM (does NOT destroy it). */
void ipc_detach(SharedData *shm);

/* Destroy SHM and semaphore. Only call from main when shutting down. */
void ipc_destroy(void);

/* ---------- Semaphore operations ---------- */

/* Acquire the semaphore (enter critical section). Blocks if held by another
 * process. */
void sem_lock(void);

/* Release the semaphore (exit critical section). */
void sem_unlock(void);

#endif /* IPC_H */