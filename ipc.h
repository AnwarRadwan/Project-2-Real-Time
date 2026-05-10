#ifndef IPC_H
#define IPC_H

#include "shared.h"
#include <time.h>


/* ---------- Message structure for message queue ---------- */
typedef struct {
  long mtype;    // يحدد المستقبل (MSG_CMD, MSG_LOG, ...)
  int source;    // NORTH, SOUTH, CONTROLLER, LOGGER, إلخ
  int direction; // أي اتجاه يتعلق الأمر
  int value;     // قيمة الأمر (RED/GREEN/...) أو عدد السيارات
  char message[128];
  time_t timestamp;
} Message;

/* ---------- SHM + Semaphore (موجودة) ---------- */
SharedData *ipc_init(void);
SharedData *ipc_attach(void);
void ipc_detach(SharedData *shm);
void ipc_destroy(void);
void sem_lock(void);
void sem_unlock(void);

/* ---------- Message Queue (جديد) ---------- */
int msg_create(void);             // ينشئ الـ message queue (من main)
int msg_open(void);               // يفتح موجودة (للعمليات الأخرى)
int msg_send(const Message *msg); // يرسل رسالة
int msg_recv(Message *msg, long mtype,
             int block); // يستقبل (block=1 حظر، 0 non-block)
void msg_destroy(void);  // يحذف الـ queue

#endif