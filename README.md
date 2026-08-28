# 🚨 AUTO GUARD — Smart Emergency Response System

> **AI-powered accident detection and emergency response system for two-wheelers**

Auto Guard is a smart safety system designed to detect motorcycle/two-wheeler accidents in real time and automatically initiate an emergency response.

The system combines an **ESP32-based embedded device**, **MPU9250 motion sensing**, **GPS**, **GSM**, **Bluetooth Classic**, **Edge Impulse machine learning**, and an **Android companion application**.

The primary objective is to reduce the delay between an accident and emergency notification by automatically detecting an accident, determining its severity, obtaining the best available location, and sending an emergency alert to registered contacts.

---

## 📌 Project Overview

Motorcycle accidents can become critical when emergency responders or family members are not immediately informed.

Auto Guard addresses this problem through a two-part architecture:

```text
                 AUTO GUARD SYSTEM
                        │
          ┌─────────────┴─────────────┐
          │                           │
       ESP32 SYSTEM              ANDROID APP
          │                           │
   ┌──────┼────────┐            ┌─────┼─────┐
   │      │        │            │     │     │
  MPU    GPS      GSM          BT   Phone   SMS
 9250            Module              GPS
   │                             
   │
 Edge Impulse ML
   │
 Accident Detection
```

The ESP32 continuously monitors motion and driving conditions. When an accident is detected, the system determines the accident severity using the trained ML model.

The Android application acts as the rider-side communication and emergency management interface.

---

# ✨ Key Features

## 🧠 AI-Based Accident Detection

The ESP32 continuously reads motion data from the MPU9250 and evaluates:

* Speed
* Pitch
* Roll
* Tilt
* Acceleration magnitude
* Change in pitch
* Change in roll
* Change in speed
* Impact condition

These features are passed to an **Edge Impulse machine-learning classifier** to determine accident severity.

Supported severity states include:

* `MODERATE`
* `SEVERE`
* `UNKNOWN`

---

## 🚨 Automatic Accident Detection

The system uses a combination of rule-based detection and machine learning.

The ESP32 first identifies a possible accident using sensor thresholds.

If an accident condition is detected:

```text
Accident detected
       ↓
10-second cancellation window
       ↓
Button pressed?
   ┌───┴───┐
  YES      NO
   ↓        ↓
Cancel    Run ML
            ↓
      Determine severity
            ↓
       Send alert
```

This prevents accidental alerts when the rider is able to cancel the event.

---

# 📱 Android Companion Application

The Android application is the rider's companion interface.

The application provides:

* Bluetooth connection with ESP32
* Accident alert reception
* Emergency contact management
* Rider profile
* Phone GPS fallback
* Emergency SMS handling
* Accident history
* Alert details
* Bike location tracking
* `Find My Bike` functionality

The application is built using modern Android development technologies including:

* Kotlin
* Jetpack Compose
* Material 3
* Kotlin Coroutines
* StateFlow
* Room Database
* Google Fused Location Provider
* Bluetooth Classic RFCOMM/SPP

---

# 📡 Bluetooth Communication

The ESP32 communicates with the Android application using **Classic Bluetooth SPP (Serial Port Profile)**.

The ESP32 Bluetooth device is identified as:

```text
AUTO_GUARD
```

The Android application connects using the standard SPP UUID:

```text
00001101-0000-1000-8000-00805F9B34FB
```

### Bluetooth Accident Packet

When Bluetooth is connected, the ESP32 sends an accident packet to the Android application.

Example:

```text
AUTOGUARD|ACCIDENT|SEVERE|SPEED:82|GPS:YES|LAT:13.082680|LON:80.270718
```

The Android application parses the packet and extracts:

```text
Event Type
Severity
Speed
ESP32 GPS availability
Latitude
Longitude
```

---

# 📍 Location Selection Logic

Auto Guard uses a fallback mechanism to obtain the best available location.

```text
             ACCIDENT
                 ↓
        ESP32 detects accident
                 ↓
        Bluetooth available?
             /          \
           YES           NO
           ↓              ↓
      Android App       GSM
           ↓
    ESP32 GPS available?
        /          \
      YES           NO
      ↓             ↓
 ESP32 GPS      Phone GPS
      └──────┬──────┘
             ↓
       Emergency Alert
```

### Location Priority

The system follows this priority:

1. **ESP32 GPS**
2. **Rider phone GPS**
3. Location marked unavailable if neither source is available

This avoids sending fake coordinates such as:

```text
0.000000, 0.000000
```

---

# 📲 Emergency SMS

Emergency contacts are configured through the **Rider Profile** inside the Android application.

The application can store emergency contact numbers and use them when an accident is received through Bluetooth.

Example emergency message:

```text
AUTO GUARD EMERGENCY ALERT

SEVERE ACCIDENT DETECTED

Rider: Sanjay
Bike: TN XX XXXX
Blood Group: O+
Speed: 82 km/h

Location Source: ESP32

Location:
Latitude: 13.082680
Longitude: 80.270718

Google Maps:
https://maps.google.com/?q=13.082680,80.270718

Medical Notes:
...

Please respond immediately.
```

The SMS contains a Google Maps-compatible location link whenever coordinates are available.

---

# 🛰️ Find My Bike — LOC Feature

Auto Guard also provides a bike-location request mechanism.

The Android application contains a:

```text
Find My Bike
```

screen with a:

```text
LOC
```

button.

When the rider requests the bike location:

```text
Android App
     ↓
LOC button
     ↓
SMS: LOC
     ↓
GSM Module
     ↓
ESP32
     ↓
GPS
     ↓
GSM Module
     ↓
Android Phone
```

The ESP32 responds using the following format:

```text
Bike Location | Lat: 9.947192 | Lon: 78.818826
```

The Android application recognizes this message, extracts the coordinates, stores the location, and displays it in the **Find My Bike** interface.

The location can also be opened in Google Maps.

---

# 🧭 LOC Communication Flow

```text
             FIND MY BIKE
                   │
                   ↓
              Press LOC
                   │
                   ↓
        Android sends SMS "LOC"
                   │
                   ↓
              GSM Network
                   │
                   ↓
               ESP32 GSM
                   │
                   ↓
             Read SMS "LOC"
                   │
                   ↓
             Read GPS data
                   │
                   ↓
       Generate bike location
                   │
                   ↓
              GSM Network
                   │
                   ↓
             Android Phone
                   │
                   ↓
       Parse "Bike Location"
                   │
                   ↓
          Display coordinates
                   │
                   ↓
             View on Map
```

---

# 🔧 Hardware Components

| Component     | Purpose                               |
| ------------- | ------------------------------------- |
| ESP32         | Main embedded controller              |
| MPU9250       | Motion, acceleration and tilt sensing |
| GPS Module    | Obtains bike coordinates              |
| GSM Module    | SMS communication                     |
| Bluetooth     | ESP32 ↔ Android communication         |
| Potentiometer | Speed simulation/input in prototype   |
| Buzzer        | Accident warning                      |
| RGB LED       | System status indication              |
| Push Button   | Accident alert cancellation           |

---

# 🔌 ESP32 Pin Configuration

The current firmware uses the following pin assignments:

| Function              | ESP32 Pin |
| --------------------- | --------: |
| Potentiometer / Speed |   GPIO 34 |
| Cancel Button         |   GPIO 19 |
| Buzzer                |   GPIO 23 |
| Red LED               |   GPIO 18 |
| Green LED             |   GPIO 26 |
| Blue LED              |   GPIO 25 |
| I2C SDA               |   GPIO 21 |
| I2C SCL               |   GPIO 22 |
| GPS RX                |    GPIO 4 |
| GPS TX                |    GPIO 5 |
| GSM RX                |   GPIO 16 |
| GSM TX                |   GPIO 17 |

### I2C

```text
ESP32 GPIO 21 → MPU9250 SDA
ESP32 GPIO 22 → MPU9250 SCL
```

### GPS

```text
ESP32 GPIO 4 → GPS TX
ESP32 GPIO 5 → GPS RX
```

### GSM

```text
ESP32 GPIO 16 → GSM TX
ESP32 GPIO 17 → GSM RX
```

> Ensure the GSM module and ESP32 use compatible voltage levels and have a suitable power supply. GSM modules can require significantly more current during transmission than the ESP32.

---

# 💡 LED Status

The RGB LED provides a simple visual indication of the system state.

| State    | LED      |
| -------- | -------- |
| Normal   | 🟢 Green |
| Warning  | 🔵 Blue  |
| Accident | 🔴 Red   |

---

# 🔊 Accident Cancellation

When an accident is detected, the system activates:

* Red LED
* Buzzer

A **10-second cancellation window** is provided.

```text
Accident detected
       ↓
Buzzer ON
Red LED ON
       ↓
Wait 10 seconds
       ↓
Button pressed?
   ┌───┴───┐
  YES      NO
   ↓        ↓
Cancel    Continue
```

If the button is pressed during the window, the emergency alert is cancelled.

---

# 🤖 Machine Learning

The accident severity classifier is implemented using **Edge Impulse**.

The ESP32 sends nine features to the classifier:

```text
1. Speed
2. Pitch
3. Roll
4. Acceleration
5. Tilt
6. Change in Pitch
7. Change in Roll
8. Change in Speed
9. Impact
```

The classifier produces probability values for the trained classes.

A minimum confidence threshold is applied before accepting a severity classification.

Current firmware configuration:

```text
ML_MIN_CONFIDENCE = 0.55
```

If the classifier fails or produces insufficient confidence, the system falls back to:

```text
UNKNOWN
```

rather than incorrectly claiming a severity.

---

# ⚙️ Accident Detection Thresholds

The current firmware uses:

```text
WARNING_SPEED = 70 km/h
WARNING_TILT = 20°
ACCIDENT_TILT = 35°
MIN_SPEED = 5 km/h
CANCEL_TIME = 10 seconds
```

Impact detection is triggered when:

```text
Acceleration > 2.0
```

or:

```text
Absolute speed change > 30
```

The actual accident decision combines the speed, tilt and impact conditions.

---

# 🏗️ Software Architecture

## ESP32 Firmware

The ESP32 firmware is responsible for:

```text
Sensor Acquisition
       ↓
Motion Processing
       ↓
Accident Detection
       ↓
Edge Impulse Classification
       ↓
Severity Determination
       ↓
Communication Router
   ┌───┴────┐
 Bluetooth  GSM
   │         │
 Android   SMS
```

Major firmware modules include:

* MPU9250 sensor handling
* GPS handling
* GSM handling
* Bluetooth communication
* Accident detection
* Edge Impulse inference
* LED state management
* Buzzer control
* Cancellation button
* LOC request processing

---

# 📱 Android Architecture

The Android application follows a layered structure.

```text
┌──────────────────────────┐
│       Jetpack Compose    │
│          UI Layer        │
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│       ViewModel Layer    │
│   State + Event Handling │
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│       Service / Helper   │
│ Bluetooth / GPS / SMS    │
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│       Room Database      │
│      Local Persistence   │
└──────────────────────────┘
```

Important application components include:

```text
BluetoothManager.kt
AccidentEvent.kt
LocationHelper.kt
SmsSender.kt
SmsReceiver.kt
SmsParser.kt
MainViewModel.kt
FindBikeScreen.kt
AlertDetailScreen.kt
```

---

# 🗃️ Local Data Storage

The Android application uses **Room Database** for local persistence.

It can store information such as:

* Rider profile
* Emergency contacts
* Accident alerts
* Accident severity
* Speed
* Coordinates
* Location source
* SMS status
* Bike location responses

This allows accident events and bike-location information to remain available inside the application.

---

# 👤 Rider Profile

The Rider Profile stores information used during emergency communication.

Typical information includes:

```text
Rider Name
Bike Number
Blood Group
Medical Notes
Emergency Contact 1
Emergency Contact 2
Bike GSM Number
```

The emergency contacts are used for accident notifications.

The Bike GSM Number is used specifically for the **Find My Bike → LOC** request.

---

# 🔐 Android Permissions

The application requires appropriate Android permissions for its communication and location features.

Relevant permissions include:

```text
BLUETOOTH
BLUETOOTH_ADMIN
BLUETOOTH_SCAN
BLUETOOTH_CONNECT

ACCESS_FINE_LOCATION
ACCESS_COARSE_LOCATION

SEND_SMS
RECEIVE_SMS
READ_SMS
```

Depending on Android version, runtime permission approval is required.

For the complete system to operate correctly, the rider should allow:

* Bluetooth access
* Location access
* SMS access

and keep the phone's location service enabled.

---

# 📂 Repository Structure

The recommended project structure is:

```text
Auto-Guard-Companion/
│
├── app/
│   └── Android application source
│
├── esp32/
│   └── AUTO_GUARD_FINAL_CLEAN.cpp
│
├── docs/
│   ├── ARCHITECTURE.md
│   ├── HARDWARE_CONNECTIONS.md
│   └── SYSTEM_FLOW.md
│
├── gradle/
│   └── wrapper/
│
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── README.md
```

---

# 🚀 Android Installation

## Requirements

* Android Studio
* Android device with Bluetooth
* Android device with SMS capability
* GPS/location capability
* ESP32 hardware
* GSM module/SIM
* GPS module

## Steps

### 1. Clone the repository

```bash
git clone https://github.com/Sanjay-06-16/Auto-Guard-Companion.git
```

### 2. Open the project

Open the cloned folder using Android Studio.

### 3. Sync Gradle

Allow Android Studio to download and configure the required dependencies.

### 4. Connect an Android device

Enable:

```text
Developer Options
USB Debugging
```

### 5. Build and run

Run the application on the physical Android device.

> A physical device is recommended because Bluetooth, GPS and SMS functionality cannot be fully represented by the Android emulator.

---

# 🔧 ESP32 Setup

The ESP32 firmware requires the appropriate Arduino libraries and the generated Edge Impulse inference library.

Required libraries include:

```text
Wire
MPU9250_asukiaaa
TinyGPSPlus
HardwareSerial
Edge Impulse inference library
```

Upload the firmware to the ESP32 using Arduino IDE.

After uploading:

1. Power the ESP32.
2. Insert the GSM SIM.
3. Ensure the GPS module has a clear view of the sky.
4. Pair the ESP32 Bluetooth device with the Android phone.
5. The Bluetooth device should appear as:

```text
AUTO_GUARD
```

---

# 📲 Pairing Bluetooth

Before using Bluetooth accident communication:

```text
Android Settings
      ↓
Bluetooth
      ↓
Search devices
      ↓
AUTO_GUARD
      ↓
Pair
      ↓
Open Auto Guard Companion
      ↓
Connect Bluetooth
```

The Android application uses Bluetooth Classic SPP to communicate with the ESP32.

---

# 🧪 Testing

## Test 1 — Bluetooth Connection

Expected:

```text
ESP32: AUTO_GUARD
        ↓
Android: CONNECTED
```

---

## Test 2 — Accident Detection

Trigger the configured accident condition.

Expected:

```text
ACCIDENT DETECTED
       ↓
RED LED
       ↓
BUZZER
       ↓
10 second cancellation
       ↓
ML classification
       ↓
Bluetooth packet
       ↓
Android app
```

---

## Test 3 — Phone GPS Fallback

Test with ESP32 GPS unavailable.

Expected:

```text
ESP32 GPS = unavailable
       ↓
Android receives accident
       ↓
Phone GPS requested
       ↓
Rider Phone GPS used
       ↓
Emergency SMS contains coordinates
```

---

## Test 4 — Emergency SMS

Configure emergency contacts in Rider Profile.

Trigger an accident.

Expected:

```text
Accident
   ↓
Android
   ↓
Location selected
   ↓
Emergency contact
   ↓
SMS
```

The SMS should contain:

```text
Accident severity
Rider information
Bike information
Speed
Location source
Coordinates
Google Maps link
Medical notes
```

---

## Test 5 — Find My Bike

Configure the Bike GSM Number in Rider Profile.

Open:

```text
Find My Bike
```

Press:

```text
LOC
```

Expected:

```text
Android → LOC SMS → ESP32 GSM
                       ↓
                      GPS
                       ↓
              Bike Location SMS
                       ↓
                   Android
```

Example response:

```text
Bike Location | Lat: 9.947192 | Lon: 78.818826
```

The application should then display the bike coordinates and provide a map option.

---

# 🛠️ Troubleshooting

## Bluetooth does not connect

Check:

* Bluetooth is enabled.
* ESP32 is powered.
* `AUTO_GUARD` is paired.
* Android Bluetooth permissions are granted.
* The ESP32 uses Classic Bluetooth SPP.
* The Android app is connecting using the SPP UUID.

---

## GPS shows unavailable

Check:

* GPS module wiring.
* GPS power supply.
* GPS RX/TX connections.
* Outdoor sky visibility.
* GPS has enough time to obtain a fix.

A GPS module may take time to acquire its first fix.

---

## Emergency SMS does not contain location

Check:

```text
Android Location permission
        ↓
Phone Location Services
        ↓
ESP32 GPS availability
        ↓
LocationHelper
        ↓
SMS generation
```

The system should prefer ESP32 GPS and use phone GPS as fallback.

---

## LOC does not work

Check:

1. Bike GSM number is saved in Rider Profile.
2. SIM is inserted into the ESP32 GSM module.
3. GSM network is available.
4. ESP32 is receiving SMS.
5. The Android app sends exactly:

```text
LOC
```

6. ESP32 GPS has a valid fix.
7. ESP32 sends:

```text
Bike Location | Lat: X | Lon: Y
```

8. Android SMS permissions are enabled.

---

# 🔄 Complete System Workflow

## Accident Workflow

```text
                   MOTORCYCLE
                       │
                       ↓
                 MPU9250 Sensor
                       │
                       ↓
                 ESP32 Processing
                       │
                       ↓
                Accident Detected
                       │
                       ↓
               10 Second Cancel
                       │
                 ┌─────┴─────┐
               Cancel       Continue
                 │             │
                STOP           ↓
                         Edge Impulse
                              ML
                               │
                               ↓
                           Severity
                               │
                               ↓
                     Bluetooth Available?
                       ┌───────┴───────┐
                      YES              NO
                       │                │
                       ↓                ↓
                  Android App         GSM
                       │
                       ↓
                  Location
                       │
             ┌─────────┴─────────┐
             ↓                   ↓
         ESP32 GPS           Phone GPS
             └─────────┬─────────┘
                       ↓
                Emergency SMS
                       ↓
                Emergency Contact
```

---

# 🛰️ Bike Tracking Workflow

```text
                 Rider Phone
                      │
                      ↓
                Find My Bike
                      │
                      ↓
                    LOC
                      │
                      ↓
                 GSM Network
                      │
                      ↓
                ESP32 GSM
                      │
                      ↓
                  GPS Module
                      │
                      ↓
                Current Location
                      │
                      ↓
                 GSM Network
                      │
                      ↓
                 Rider Phone
                      │
                      ↓
             Parse Bike Location
                      │
                      ↓
                 Show on Map
```

---

# 🔒 Safety Considerations

Auto Guard is a prototype emergency-response system and should not be treated as a certified automotive safety system.

Important considerations for future production deployment include:

* Reliable automotive-grade power management
* Waterproof hardware enclosure
* Cellular coverage monitoring
* GPS accuracy validation
* Battery backup
* Secure communication
* False-positive reduction
* Extensive real-world accident testing
* Automotive-grade sensors
* Certified emergency communication infrastructure

The current prototype is intended for educational, research, demonstration, and hackathon purposes.

---

# 📈 Future Improvements

Potential future improvements include:

* Advanced accident classification
* Better sensor fusion
* Automatic crash image capture
* Cloud-based emergency monitoring
* Emergency-service integration
* Real-time tracking dashboard
* Multiple vehicle management
* Offline event queueing
* Improved GPS filtering
* Cellular data communication
* Secure encrypted Bluetooth communication
* Automatic battery-health monitoring
* OTA firmware updates
* Production-grade enclosure and power management

---

# 🎯 Project Objectives

The project aims to:

1. Detect two-wheeler accidents automatically.
2. Use machine learning to classify accident severity.
3. Provide a rider cancellation mechanism.
4. Communicate accident information to the Android application.
5. Select the best available location source.
6. Notify registered emergency contacts.
7. Allow the rider to locate the bike remotely using the LOC feature.
8. Reduce the delay between accident detection and emergency notification.

---

# 🏆 Technologies Used

### Embedded System

```text
ESP32
MPU9250
GPS
GSM
Classic Bluetooth
Arduino Framework
```

### Artificial Intelligence

```text
Edge Impulse
Embedded Machine Learning
Sensor-based Classification
```

### Android

```text
Kotlin
Jetpack Compose
Material 3
Kotlin Coroutines
StateFlow
Room Database
Google Fused Location Provider
Bluetooth Classic SPP
Android SMS APIs
```

### Communication

```text
Bluetooth SPP
GSM SMS
GPS
```

---

# 👨‍💻 Project

**Project:** Auto Guard — Smart Emergency Response System

**Repository:**
https://github.com/Sanjay-06-16/Auto-Guard-Companion

**Platform:** Android + ESP32

**Domain:** Artificial Intelligence / IoT / Embedded Systems / Mobile Application / Road Safety

---

# 📄 License

This project is currently intended primarily for educational, research, prototype, and demonstration purposes.

A formal open-source license can be added to this repository if the project is released for external use.

---

# ⭐ Project Summary

Auto Guard combines **Artificial Intelligence, Embedded Systems, GPS, GSM, Bluetooth, and Android technologies** into a single two-wheeler emergency-response platform.

The system continuously monitors rider motion using the ESP32 and MPU9250. When an accident is detected, an Edge Impulse model determines the likely severity. The Android application receives the event through Bluetooth and selects the best available location using ESP32 GPS or the rider's phone GPS.

The system then generates an emergency SMS containing the accident details and location.

In addition, the **Find My Bike** feature allows the rider to request the bike's current GPS position remotely using an SMS-based `LOC` command.

The overall architecture provides two independent safety mechanisms:

```text
ACCIDENT RESPONSE
ESP32 → Bluetooth → Android → Location → Emergency SMS

BIKE LOCATION
Android → LOC SMS → ESP32 → GPS → GSM → Android
```

This makes Auto Guard a complete prototype for intelligent two-wheeler accident detection, emergency communication, and remote bike location tracking.
