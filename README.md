# HealthBridge 

> A WHOOP-alternative personal health intelligence app 
> built by reverse engineering the FitCloud Pro BLE protocol

## What This Is
A fully standalone Android health tracking app that reads 
directly from FireBoltt smartwatches over BLE — no cloud 
dependency, no subscription, your data stays with you.

## How It Was Built
Most health apps use official SDKs or cloud APIs.
This one doesn't.

1. **BLE Interception** — Used Burp Suite + Android proxy
   to intercept smartwatch traffic. Hit certificate pinning.

2. **Protocol Reverse Engineering** — Cloned 
   htangsmart/FitCloudPro-SDK-Android, decompiled 
   sdk-fitcloud-v3.0.2.aar using jadx, extracted:
   - Proprietary GATT UUID mappings
   - Byte packet structure (header + items)
   - Timestamp calculation formula
   - Data type encodings for HR, steps, sleep, SpO2

3. **Android App** — Built using decoded protocol +
   official SDK for connection management

4. **AI Layer** — Groq (Llama 3) generates personalized
   recovery coaching using actual sensor data

## Features
-  Live HR + SpO2 real-time charts
-  Recovery Score (athlete-calibrated)
-  AI health coaching (knows your name + sport)
-  Strain score from workouts  
-  Sleep tracking + manual input
-  7-day recovery trend
-  Ubuntu data pipeline (FastAPI + SQLite)
-  Fitness profile (Sedentary → Elite athlete)

## Accuracy
HR and SpO2 readings are **more accurate** than the 
official FitCloud Pro app in testing.

## Tech Stack
- Android (Java), Room DB, RxJava3
- FitCloud Pro SDK (htangsmart)
- Groq API (Llama 3.1)
- FastAPI + SQLite (Ubuntu receiver)
- BLE protocol decoded from SDK source

## Supported Devices
- FireBoltt Crusader AMOLED (primary)
- More devices coming (adding WatchProvider abstraction)

## Architecture
