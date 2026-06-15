# Project 2 - Real-Time System

## 📌 Overview

This project is a real-time traffic control and monitoring system implemented in C.
It simulates multiple components working together such as traffic lights, vehicle detection, pedestrian control, emergency handling, and safety monitoring.

The system demonstrates inter-process communication (IPC) and concurrent execution in a real-time environment.

---

## ⚙️ Features

* 🚦 Traffic light control system
* 🚗 Vehicle detection module
* 🚶 Pedestrian crossing management
* 🚨 Emergency handling system
* 🛡️ Safety monitoring module
* 📡 Inter-process communication (IPC)
* 🧾 Logging system for events
* 🖥️ Optional OpenGL UI visualization

---

## 🧱 Project Structure

* `main.c` → Main controller of the system
* `traffic_light.c` → Traffic light logic
* `vehicle_detector.c` → Vehicle detection simulation
* `pedestrian.c` → Pedestrian handling
* `emergency.c` → Emergency scenarios
* `safety_monitor.c` → Safety checks
* `ipc.c / ipc.h` → Communication between modules
* `logger.c` → Logging system
* `opengl_ui.c` → Visualization (if enabled)
* `Makefile` → Build automation

---

## 🛠️ How to Build

Use the Makefile:

```bash id="q3m8ka"
make
```

---

## ▶️ How to Run

```bash id="p7k1za"
./main
```

---

## 📦 Requirements

* GCC compiler
* Linux environment (recommended: Ubuntu / WSL)
* Make

Optional:

* OpenGL libraries (for UI module)

---

## 👨‍💻 Authors

* Anwar Atawna
* Qusai Abo Sundos
* Wael Sehwiel
* Yousef Hilal

---

## 📊 Notes

* Educational real-time system project
* Demonstrates IPC and concurrent processes
* Can be extended with GUI or networking features
