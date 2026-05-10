/*
 * traffic_light.c
 * ----------------
 * One process per direction (NORTH/SOUTH/EAST/WEST).
 * Usage: ./traffic_light <direction>
 *   where direction = 0 (N), 1 (S), 2 (E), 3 (W)
 *
 * Responsibilities (per project spec, Section 2):
 *  - Maintain awareness of its current light state (read from SHM).
 *  - Receive control commands from the Controller via MSG_CMD.
 *  - Report state changes (print to terminal).
 *  - Send MSG_CONFIRM back to the Controller.
 *  - Send MSG_LOG to the logger when state changes.
 *  - Report errors if SHM state does not match the commanded state.
 */

#include <errno.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>


#include "config.h"
#include "ipc.h"
#include "shared.h"


/* ---------- Global state for this process ---------- */
static int g_direction = -1;       /* which direction this process handles */
static SharedData *g_shm = NULL;   /* attached shared memory */
static int g_last_seen_light = -1; /* last light state we observed */
static volatile sig_atomic_t g_running = 1;

/* ---------- Signal handler for graceful shutdown ---------- */
static void on_sigterm(int sig) {
  (void)sig;
  g_running = 0;
}

/* ---------- Helper: send confirmation back to controller ---------- */
static void send_confirmation(int new_state) {
  Message m;
  memset(&m, 0, sizeof(m));
  m.mtype = MSG_CONFIRM;
  m.source = g_direction; /* who is reporting */
  m.direction = g_direction;
  m.value = new_state;
  m.timestamp = time(NULL);
  snprintf(m.message, sizeof(m.message), "Light %s changed to %s",
           dir_str(g_direction), light_str(new_state));

  if (msg_send(&m) < 0) {
    fprintf(stderr, "[LIGHT %s] failed to send confirmation\n",
            dir_str(g_direction));
  }
}

/* ---------- Helper: send log message ---------- */
static void send_log(const char *text) {
  Message m;
  memset(&m, 0, sizeof(m));
  m.mtype = MSG_LOG;
  m.source = g_direction;
  m.direction = g_direction;
  m.timestamp = time(NULL);
  snprintf(m.message, sizeof(m.message), "%s", text);
  /* best-effort: don't block on logger */
  msg_send(&m);
}

/* ---------- Handle a command targeted at this light ---------- */
static void handle_command(const Message *cmd) {
  /* The controller sends: direction = which light, value = new state */
  if (cmd->direction != g_direction) {
    /* Not for us — but we already filtered by mtype, so this shouldn't happen.
     * Keeping the check as a safety net. */
    return;
  }

  int requested = cmd->value;
  if (requested != RED && requested != YELLOW && requested != GREEN) {
    fprintf(stderr, "[LIGHT %s] invalid state requested: %d\n",
            dir_str(g_direction), requested);
    return;
  }

  /* Verify SHM matches the command (the controller writes SHM directly,
   * then sends us the command — so by the time we read this message,
   * SHM should already reflect the new state). */
  sem_lock();
  int actual = g_shm->light[g_direction];
  sem_unlock();

  if (actual != requested) {
    char err[128];
    snprintf(err, sizeof(err), "MISMATCH: cmd=%s but SHM=%s",
             light_str(requested), light_str(actual));
    fprintf(stderr, "[LIGHT %s] %s\n", dir_str(g_direction), err);
    send_log(err);
    return;
  }

  /* All good — print and confirm */
  printf("[LIGHT %s] %s -> %s\n", dir_str(g_direction),
         light_str(g_last_seen_light), light_str(requested));
  fflush(stdout);

  g_last_seen_light = requested;
  send_confirmation(requested);

  char log_msg[128];
  snprintf(log_msg, sizeof(log_msg), "%s changed to %s", dir_str(g_direction),
           light_str(requested));
  send_log(log_msg);
}

/* ---------- Periodic SHM check (catches state changes we missed) ---------- */
static void poll_shm_state(void) {
  sem_lock();
  int current = g_shm->light[g_direction];
  int shutdown = g_shm->shutdown;
  sem_unlock();

  if (shutdown) {
    g_running = 0;
    return;
  }

  /* If SHM changed but we never got a command (e.g. controller updated
   * SHM but message was lost), still print and log so the user sees it. */
  if (current != g_last_seen_light && g_last_seen_light != -1) {
    printf("[LIGHT %s] (observed) %s -> %s\n", dir_str(g_direction),
           light_str(g_last_seen_light), light_str(current));
    fflush(stdout);

    char log_msg[128];
    snprintf(log_msg, sizeof(log_msg),
             "%s observed change to %s (no cmd received)", dir_str(g_direction),
             light_str(current));
    send_log(log_msg);

    g_last_seen_light = current;
  } else if (g_last_seen_light == -1) {
    /* First poll — just record what we see */
    g_last_seen_light = current;
    printf("[LIGHT %s] initial state: %s\n", dir_str(g_direction),
           light_str(current));
    fflush(stdout);
  }
}

/* ---------- Main loop ---------- */
int main(int argc, char *argv[]) {
  if (argc != 2) {
    fprintf(stderr, "Usage: %s <direction 0..3>\n", argv[0]);
    return 1;
  }

  g_direction = atoi(argv[1]);
  if (g_direction < 0 || g_direction > 3) {
    fprintf(stderr, "Invalid direction: %d (must be 0..3)\n", g_direction);
    return 1;
  }

  /* Install signal handlers */
  signal(SIGTERM, on_sigterm);
  signal(SIGINT, on_sigterm);

  /* Attach to existing IPC (created by main.c) */
  g_shm = ipc_attach();
  if (!g_shm) {
    fprintf(stderr, "[LIGHT %s] ipc_attach failed\n", dir_str(g_direction));
    return 1;
  }

  printf("[LIGHT %s] started (pid=%d)\n", dir_str(g_direction), getpid());
  fflush(stdout);

  char start_msg[128];
  snprintf(start_msg, sizeof(start_msg), "Traffic light process %s started",
           dir_str(g_direction));
  send_log(start_msg);

  /* Main loop: non-blocking message receive + periodic SHM check */
  while (g_running) {
    Message cmd;

    /* Try to receive a command targeted at this direction.
     *
     * Trick: we use mtype = MSG_CMD * 10 + direction + 1 so each
     * light can filter only its own commands. The controller must
     * send commands with this encoding.
     *
     * Alternative: use mtype = MSG_CMD and check direction inside —
     * but then ALL lights would race for the same message.
     * The encoded mtype lets msgrcv() filter at the kernel level.
     */
    long my_mtype = MSG_CMD * 10 + g_direction + 1;

    int n = msg_recv_type(&cmd, my_mtype, 1 /* non-blocking */);
    if (n > 0) {
      handle_command(&cmd);
    } else if (n < 0 && errno != ENOMSG) {
      /* Real error, not just "no message" */
      perror("msg_recv");
    }

    /* Periodic SHM check */
    poll_shm_state();

    /* Sleep briefly to avoid busy-wait */
    usleep(100 * 1000); /* 100 ms */
  }

  /* Cleanup */
  printf("[LIGHT %s] shutting down\n", dir_str(g_direction));
  send_log("Traffic light process shutting down");
  ipc_detach(g_shm);
  return 0;
}