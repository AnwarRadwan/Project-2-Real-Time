/*Anwar Atawna
 * config.h
 * --------
 * Constants and IPC keys for the traffic light system.
 * Stage 1: Shared Memory only (semaphores/message queues come later).
 */

#ifndef CONFIG_H
#define CONFIG_H

/* ---------- Light states ---------- */
#define RED 0
#define YELLOW 1
#define GREEN 2

/* ---------- Directions ---------- */
#define NORTH 0
#define SOUTH 1
#define EAST 2
#define WEST 3
#define NUM_DIRECTIONS 4

/* ---------- IPC keys ---------- */
#define SHM_KEY 0x1234
#define SEM_KEY 0x5678
/* MSG_KEY will be added in the next stage */

/* SEM_KEY and MSG_KEY will be added in later stages */

#endif /* CONFIG_H */