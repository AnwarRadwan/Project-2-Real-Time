/*
 * controller.c  —  Anwar Atawna
 * ------------------------------
 * The "brain" of the traffic light system.
 *
 * Full cycle (protected left-turn first):
 *   PHASE_NS_LEFT_GREEN  → PHASE_NS_LEFT_YELLOW  → PHASE_ALL_RED_3 →
 *   PHASE_NS_GREEN       → PHASE_NS_YELLOW        → PHASE_ALL_RED_1 →
 *   PHASE_EW_LEFT_GREEN  → PHASE_EW_LEFT_YELLOW  → PHASE_ALL_RED_4 →
 *   PHASE_EW_GREEN       → PHASE_EW_YELLOW        → PHASE_ALL_RED_2 → (repeat)
 *
 * Each direction carries two independent signals:
 *   light[d]      — straight-through / right-turn signal
 *   left_light[d] — protected left-turn arrow signal
 *
 * Interrupts (highest-priority first):
 *   EMERGENCY  — preempts any phase; clears via YELLOW+ALL-RED.
 *   PEDESTRIAN — preempts any green phase (straight or left).
 *
 * IPC:
 *   SHM  : controller is the ONLY writer of light[] and left_light[].
 *   MSG  : CMD messages carry both value (straight) and left_value.
 *   LOG  : every phase transition logged.
 *
 * Adaptive green: applied to straight phases only.
 * Left-turn phases use a fixed LEFT_GREEN_DURATION.
 */

#include <errno.h>
#include <signal.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

#include "config.h"
#include "ipc.h"
#include "shared.h"

static SharedData            *g_shm    = NULL;
static volatile sig_atomic_t  g_running = 1;

static int g_want_pedestrian = 0;
static int g_want_emergency  = 0;
static int g_emergency_dir   = -1;

static void on_signal(int sig) { (void)sig; g_running = 0; }

/* ------------------------------------------------------------------ */
/* Logging helper                                                       */
/* ------------------------------------------------------------------ */
static void log_event(const char *fmt, ...) {
    Message m;
    va_list ap;
    memset(&m, 0, sizeof(m));
    m.mtype     = MSG_LOG;
    m.source    = SRC_CONTROLLER;
    m.timestamp = time(NULL);
    va_start(ap, fmt);
    vsnprintf(m.message, sizeof(m.message), fmt, ap);
    va_end(ap);
    msg_send(&m);
    printf("[CTRL] %s\n", m.message);
    fflush(stdout);
}

/* ------------------------------------------------------------------ */
/* apply_phase: write both light[] and left_light[] to SHM, then      */
/*              broadcast CMD messages to all four traffic_light procs */
/* ------------------------------------------------------------------ */
static void apply_phase(int phase) {
    sem_lock();
    g_shm->current_phase    = phase;
    g_shm->phase_start_time = time(NULL);
    g_shm->last_update      = time(NULL);

    /* Default: all left-turn arrows off */
    for (int d = 0; d < NUM_DIRECTIONS; d++) g_shm->left_light[d] = RED;

    switch (phase) {

    case PHASE_NS_LEFT_GREEN:
        for (int d = 0; d < NUM_DIRECTIONS; d++) g_shm->light[d] = RED;
        g_shm->left_light[NORTH] = GREEN; g_shm->left_light[SOUTH] = GREEN;
        g_shm->pedestrian_active = 0;     g_shm->emergency_mode    = 0;
        break;

    case PHASE_NS_LEFT_YELLOW:
        for (int d = 0; d < NUM_DIRECTIONS; d++) g_shm->light[d] = RED;
        g_shm->left_light[NORTH] = YELLOW; g_shm->left_light[SOUTH] = YELLOW;
        break;

    case PHASE_NS_GREEN:
        g_shm->light[NORTH] = GREEN;  g_shm->light[SOUTH] = GREEN;
        g_shm->light[EAST]  = RED;    g_shm->light[WEST]  = RED;
        g_shm->pedestrian_active = 0; g_shm->emergency_mode = 0;
        break;

    case PHASE_NS_YELLOW:
        g_shm->light[NORTH] = YELLOW; g_shm->light[SOUTH] = YELLOW;
        g_shm->light[EAST]  = RED;    g_shm->light[WEST]  = RED;
        break;

    case PHASE_ALL_RED_1:
    case PHASE_ALL_RED_2:
    case PHASE_ALL_RED_3:
    case PHASE_ALL_RED_4:
        for (int d = 0; d < NUM_DIRECTIONS; d++) g_shm->light[d] = RED;
        break;

    case PHASE_EW_LEFT_GREEN:
        for (int d = 0; d < NUM_DIRECTIONS; d++) g_shm->light[d] = RED;
        g_shm->left_light[EAST] = GREEN; g_shm->left_light[WEST] = GREEN;
        g_shm->pedestrian_active = 0;    g_shm->emergency_mode   = 0;
        break;

    case PHASE_EW_LEFT_YELLOW:
        for (int d = 0; d < NUM_DIRECTIONS; d++) g_shm->light[d] = RED;
        g_shm->left_light[EAST] = YELLOW; g_shm->left_light[WEST] = YELLOW;
        break;

    case PHASE_EW_GREEN:
        g_shm->light[NORTH] = RED;    g_shm->light[SOUTH] = RED;
        g_shm->light[EAST]  = GREEN;  g_shm->light[WEST]  = GREEN;
        g_shm->pedestrian_active = 0; g_shm->emergency_mode = 0;
        break;

    case PHASE_EW_YELLOW:
        g_shm->light[NORTH] = RED;    g_shm->light[SOUTH] = RED;
        g_shm->light[EAST]  = YELLOW; g_shm->light[WEST]  = YELLOW;
        break;

    case PHASE_PEDESTRIAN:
        for (int d = 0; d < NUM_DIRECTIONS; d++) g_shm->light[d] = RED;
        g_shm->pedestrian_active  = 1;
        g_shm->pedestrian_request = 0;
        g_shm->emergency_mode     = 0;
        break;

    case PHASE_EMERGENCY:
        for (int d = 0; d < NUM_DIRECTIONS; d++) g_shm->light[d] = RED;
        g_shm->light[g_emergency_dir] = GREEN;
        g_shm->emergency_mode         = 1;
        g_shm->emergency_direction    = g_emergency_dir;
        g_shm->pedestrian_active      = 0;
        g_shm->pedestrian_request     = 0;
        break;
    }

    /* Snapshot both signal arrays before releasing the lock */
    int lights[NUM_DIRECTIONS], lefts[NUM_DIRECTIONS];
    for (int d = 0; d < NUM_DIRECTIONS; d++) {
        lights[d] = g_shm->light[d];
        lefts[d]  = g_shm->left_light[d];
    }
    sem_unlock();

    /* Notify each traffic_light process via its own message type */
    for (int d = 0; d < NUM_DIRECTIONS; d++) {
        Message cmd;
        build_cmd_message(&cmd, d, lights[d], lefts[d]);
        msg_send(&cmd);
    }

    log_event("Phase → %s", phase_str(phase));
}

/* ------------------------------------------------------------------ */
/* check_msgs: drain pending requests (non-blocking)                   */
/* ------------------------------------------------------------------ */
static void check_msgs(void) {
    Message m;

    while (msg_recv(&m, MSG_PEDESTRIAN, 0) > 0) {
        if (!g_want_pedestrian && !g_want_emergency) {
            g_want_pedestrian = 1;
            log_event("Pedestrian request received — will serve at next opportunity");
            sem_lock();
            g_shm->pedestrian_request = 1;
            sem_unlock();
        }
    }

    while (msg_recv(&m, MSG_EMERGENCY, 0) > 0) {
        g_want_emergency = 1;
        g_emergency_dir  = m.direction;
        log_event("EMERGENCY vehicle from %s — preempting cycle!",
                  dir_str(m.direction));
    }
}

/* ------------------------------------------------------------------ */
/* Adaptive green duration (straight phases only)                      */
/* ------------------------------------------------------------------ */
static int green_duration(int d1, int d2) {
    sem_lock();
    int n = g_shm->vehicle_count[d1] + g_shm->vehicle_count[d2];
    sem_unlock();
    int t = GREEN_DURATION + n * GREEN_TIME_PER_VEHICLE;
    if (t < MIN_GREEN_DURATION) t = MIN_GREEN_DURATION;
    if (t > MAX_GREEN_DURATION) t = MAX_GREEN_DURATION;
    return t;
}

/* ------------------------------------------------------------------ */
/* safe_to_allred: enforce GREEN→YELLOW→RED rule for all phase types   */
/* ------------------------------------------------------------------ */
static void safe_to_allred(int cur_phase) {
    switch (cur_phase) {
    case PHASE_NS_LEFT_GREEN:
        apply_phase(PHASE_NS_LEFT_YELLOW);
        for (int t = 0; t < YELLOW_DURATION && g_running; t++) sleep(1);
        apply_phase(PHASE_ALL_RED_3);
        for (int t = 0; t < ALL_RED_DURATION && g_running; t++) sleep(1);
        break;
    case PHASE_NS_LEFT_YELLOW:
        apply_phase(PHASE_ALL_RED_3);
        for (int t = 0; t < ALL_RED_DURATION && g_running; t++) sleep(1);
        break;
    case PHASE_NS_GREEN:
        apply_phase(PHASE_NS_YELLOW);
        for (int t = 0; t < YELLOW_DURATION && g_running; t++) sleep(1);
        apply_phase(PHASE_ALL_RED_1);
        for (int t = 0; t < ALL_RED_DURATION && g_running; t++) sleep(1);
        break;
    case PHASE_NS_YELLOW:
        apply_phase(PHASE_ALL_RED_1);
        for (int t = 0; t < ALL_RED_DURATION && g_running; t++) sleep(1);
        break;
    case PHASE_EW_LEFT_GREEN:
        apply_phase(PHASE_EW_LEFT_YELLOW);
        for (int t = 0; t < YELLOW_DURATION && g_running; t++) sleep(1);
        apply_phase(PHASE_ALL_RED_4);
        for (int t = 0; t < ALL_RED_DURATION && g_running; t++) sleep(1);
        break;
    case PHASE_EW_LEFT_YELLOW:
        apply_phase(PHASE_ALL_RED_4);
        for (int t = 0; t < ALL_RED_DURATION && g_running; t++) sleep(1);
        break;
    case PHASE_EW_GREEN:
        apply_phase(PHASE_EW_YELLOW);
        for (int t = 0; t < YELLOW_DURATION && g_running; t++) sleep(1);
        apply_phase(PHASE_ALL_RED_2);
        for (int t = 0; t < ALL_RED_DURATION && g_running; t++) sleep(1);
        break;
    case PHASE_EW_YELLOW:
        apply_phase(PHASE_ALL_RED_2);
        for (int t = 0; t < ALL_RED_DURATION && g_running; t++) sleep(1);
        break;
    default:
        /* Already ALL-RED or special phase — no action needed */
        break;
    }
}

/* ------------------------------------------------------------------ */
/* Handle emergency: safe clear → emergency green → resume            */
/* ------------------------------------------------------------------ */
static void handle_emergency(int *cur) {
    g_want_emergency = 0;
    log_event("Handling EMERGENCY [%s] — clearing intersection",
              dir_str(g_emergency_dir));

    safe_to_allred(*cur);
    if (!g_running) return;

    apply_phase(PHASE_EMERGENCY);
    *cur = PHASE_EMERGENCY;

    for (int t = 0; t < EMERGENCY_DURATION && g_running; t++) {
        sleep(1);
        check_msgs();
    }

    /* Emergency direction was GREEN — must pass through YELLOW */
    sem_lock();
    g_shm->light[g_emergency_dir] = YELLOW;
    g_shm->last_update = time(NULL);
    sem_unlock();
    {   Message cmd;
        build_cmd_message(&cmd, g_emergency_dir, YELLOW, RED);
        msg_send(&cmd); }
    log_event("Emergency direction %s → YELLOW (safety transition)",
              dir_str(g_emergency_dir));
    for (int t = 0; t < YELLOW_DURATION && g_running; t++) sleep(1);

    apply_phase(PHASE_ALL_RED_1);
    for (int t = 0; t < ALL_RED_DURATION && g_running; t++) sleep(1);
    apply_phase(PHASE_NS_LEFT_GREEN);
    *cur = PHASE_NS_LEFT_GREEN;
    log_event("Emergency cleared — resuming normal cycle");
    sleep(1);
}

/* ------------------------------------------------------------------ */
/* Handle pedestrian: safe clear → walk signal → resume               */
/* ------------------------------------------------------------------ */
static void handle_pedestrian(int *cur) {
    g_want_pedestrian = 0;
    log_event("Handling PEDESTRIAN — clearing intersection");

    safe_to_allred(*cur);
    if (!g_running) return;

    apply_phase(PHASE_PEDESTRIAN);
    *cur = PHASE_PEDESTRIAN;

    for (int t = 0; t < PEDESTRIAN_DURATION && g_running; t++) {
        sleep(1);
        check_msgs();
        if (g_want_emergency) return;
    }

    apply_phase(PHASE_ALL_RED_1);
    for (int t = 0; t < ALL_RED_DURATION && g_running; t++) sleep(1);
    apply_phase(PHASE_NS_LEFT_GREEN);
    *cur = PHASE_NS_LEFT_GREEN;
    log_event("Pedestrian crossing complete — resuming normal cycle");
    sleep(1);
}

/* ================================================================== */
/* main                                                                 */
/* ================================================================== */
int main(void) {
    signal(SIGTERM, on_signal);
    signal(SIGINT,  on_signal);

    g_shm = ipc_attach();
    if (!g_shm) { fprintf(stderr, "[CTRL] ipc_attach failed\n"); return 1; }

    sem_lock();
    g_shm->controller_pid = getpid();
    g_shm->running        = 1;
    sem_unlock();

    printf("[CTRL] controller started (pid=%d)\n", getpid());
    log_event("Controller started — beginning traffic cycle");

    /* Start with N-S protected left-turn */
    apply_phase(PHASE_NS_LEFT_GREEN);
    int cur = PHASE_NS_LEFT_GREEN;

    while (g_running) {
        sem_lock(); int shutdown = g_shm->shutdown; sem_unlock();
        if (shutdown) break;

        /* Emergency — highest priority */
        if (g_want_emergency) { handle_emergency(&cur); continue; }

        /* Pedestrian — interrupts during any green phase */
        if (g_want_pedestrian &&
            (cur == PHASE_NS_LEFT_GREEN || cur == PHASE_EW_LEFT_GREEN ||
             cur == PHASE_NS_GREEN      || cur == PHASE_EW_GREEN)) {
            handle_pedestrian(&cur);
            continue;
        }

        /* Normal cycle state machine */
        switch (cur) {

        /* ---- N-S LEFT-TURN phase ---- */
        case PHASE_NS_LEFT_GREEN: {
            int dur = LEFT_GREEN_DURATION;
            log_event("N-S LEFT-GREEN  dur=%ds", dur);
            for (int t = 0; t < dur && g_running; t++) {
                sleep(1);
                check_msgs();
                if (g_want_emergency || g_want_pedestrian) break;
                sem_lock(); g_shm->phase_time_remaining = dur - t - 1; sem_unlock();
            }
            if (!g_want_emergency && !g_want_pedestrian) {
                apply_phase(PHASE_NS_LEFT_YELLOW); cur = PHASE_NS_LEFT_YELLOW;
            }
            break;
        }

        case PHASE_NS_LEFT_YELLOW:
            for (int t = 0; t < YELLOW_DURATION && g_running; t++) sleep(1);
            apply_phase(PHASE_ALL_RED_3); cur = PHASE_ALL_RED_3;
            break;

        case PHASE_ALL_RED_3:
            for (int t = 0; t < ALL_RED_DURATION && g_running; t++) sleep(1);
            apply_phase(PHASE_NS_GREEN); cur = PHASE_NS_GREEN;
            break;

        /* ---- N-S STRAIGHT phase ---- */
        case PHASE_NS_GREEN: {
            int dur = green_duration(NORTH, SOUTH);
            log_event("N-S GREEN  dur=%ds  (N:%d S:%d vehicles)", dur,
                      g_shm->vehicle_count[NORTH], g_shm->vehicle_count[SOUTH]);
            for (int t = 0; t < dur && g_running; t++) {
                sleep(1);
                check_msgs();
                if (g_want_emergency || g_want_pedestrian) break;
                sem_lock(); g_shm->phase_time_remaining = dur - t - 1; sem_unlock();
            }
            if (!g_want_emergency && !g_want_pedestrian) {
                apply_phase(PHASE_NS_YELLOW); cur = PHASE_NS_YELLOW;
            }
            break;
        }

        case PHASE_NS_YELLOW:
            for (int t = 0; t < YELLOW_DURATION && g_running; t++) sleep(1);
            apply_phase(PHASE_ALL_RED_1); cur = PHASE_ALL_RED_1;
            break;

        case PHASE_ALL_RED_1:
            for (int t = 0; t < ALL_RED_DURATION && g_running; t++) sleep(1);
            apply_phase(PHASE_EW_LEFT_GREEN); cur = PHASE_EW_LEFT_GREEN;
            break;

        /* ---- E-W LEFT-TURN phase ---- */
        case PHASE_EW_LEFT_GREEN: {
            int dur = LEFT_GREEN_DURATION;
            log_event("E-W LEFT-GREEN  dur=%ds", dur);
            for (int t = 0; t < dur && g_running; t++) {
                sleep(1);
                check_msgs();
                if (g_want_emergency || g_want_pedestrian) break;
                sem_lock(); g_shm->phase_time_remaining = dur - t - 1; sem_unlock();
            }
            if (!g_want_emergency && !g_want_pedestrian) {
                apply_phase(PHASE_EW_LEFT_YELLOW); cur = PHASE_EW_LEFT_YELLOW;
            }
            break;
        }

        case PHASE_EW_LEFT_YELLOW:
            for (int t = 0; t < YELLOW_DURATION && g_running; t++) sleep(1);
            apply_phase(PHASE_ALL_RED_4); cur = PHASE_ALL_RED_4;
            break;

        case PHASE_ALL_RED_4:
            for (int t = 0; t < ALL_RED_DURATION && g_running; t++) sleep(1);
            apply_phase(PHASE_EW_GREEN); cur = PHASE_EW_GREEN;
            break;

        /* ---- E-W STRAIGHT phase ---- */
        case PHASE_EW_GREEN: {
            int dur = green_duration(EAST, WEST);
            log_event("E-W GREEN  dur=%ds  (E:%d W:%d vehicles)", dur,
                      g_shm->vehicle_count[EAST], g_shm->vehicle_count[WEST]);
            for (int t = 0; t < dur && g_running; t++) {
                sleep(1);
                check_msgs();
                if (g_want_emergency || g_want_pedestrian) break;
                sem_lock(); g_shm->phase_time_remaining = dur - t - 1; sem_unlock();
            }
            if (!g_want_emergency && !g_want_pedestrian) {
                apply_phase(PHASE_EW_YELLOW); cur = PHASE_EW_YELLOW;
            }
            break;
        }

        case PHASE_EW_YELLOW:
            for (int t = 0; t < YELLOW_DURATION && g_running; t++) sleep(1);
            apply_phase(PHASE_ALL_RED_2); cur = PHASE_ALL_RED_2;
            break;

        case PHASE_ALL_RED_2:
            for (int t = 0; t < ALL_RED_DURATION && g_running; t++) sleep(1);
            apply_phase(PHASE_NS_LEFT_GREEN); cur = PHASE_NS_LEFT_GREEN;
            break;

        default:
            log_event("Unexpected phase %d — recovering", cur);
            apply_phase(PHASE_ALL_RED_1); cur = PHASE_ALL_RED_1;
            break;
        }
    }

    /* All lights to RED on exit */
    for (int d = 0; d < NUM_DIRECTIONS; d++) {
        Message cmd;
        sem_lock();
        g_shm->light[d]      = RED;
        g_shm->left_light[d] = RED;
        sem_unlock();
        build_cmd_message(&cmd, d, RED, RED);
        msg_send(&cmd);
    }
    sem_lock(); g_shm->running = 0; sem_unlock();

    log_event("Controller shut down — all lights RED");
    ipc_detach(g_shm);
    return 0;
}
