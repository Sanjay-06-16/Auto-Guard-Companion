// ============================================================
// LIBRARIES
// ============================================================

#include <Wire.h>
#include <MPU9250_asukiaaa.h>
#include <TinyGPSPlus.h>
#include <HardwareSerial.h>
#include <BluetoothSerial.h>
#include <math.h>

#include <AUTO_GUARD_Accident_Detection_inferencing.h>
#include "edge-impulse-sdk/classifier/ei_run_classifier.h"

// ============================================================
// PIN CONFIGURATION
// ============================================================

#define POT_PIN       34
#define BUTTON_PIN    19
#define BUZZER        23
#define LED_R         18
#define LED_G         26
#define LED_B         25
#define SDA_PIN       21
#define SCL_PIN       22
#define GPS_RX        4
#define GPS_TX        5
#define GSM_RX        16
#define GSM_TX        17

// ============================================================
// OBJECTS
// ============================================================

MPU9250_asukiaaa mpu;
TinyGPSPlus gps;
HardwareSerial gpsSerial(1);
HardwareSerial gsm(2);
BluetoothSerial SerialBT;

// ============================================================
// SETTINGS
// ============================================================

// ------------------------------------------------------------
// GSM fallback emergency numbers
//
// These are ONLY used when Bluetooth is NOT connected.
// ------------------------------------------------------------

String phone1 = "+918012776655";
String phone2 = "+919655240031";

// ------------------------------------------------------------
// Bluetooth name
// ------------------------------------------------------------

const char* BT_DEVICE_NAME = "AUTO_GUARD";

// ============================================================
// ACCIDENT THRESHOLDS
// ============================================================

#define WARNING_SPEED       70
#define WARNING_TILT        20
#define ACCIDENT_TILT       35
#define MIN_SPEED           5
#define CANCEL_TIME         10000

// Minimum Edge Impulse confidence
#define ML_MIN_CONFIDENCE   0.55

// Serial logging interval
#define LOOP_LOG_INTERVAL   500

// ============================================================
// SENSOR VARIABLES
// ============================================================

float prevPitch = 0;
float prevRoll = 0;
float prevAccel = 1;
float prevSpeed = 0;

unsigned long lastLoopLog = 0;

// ============================================================
// SYSTEM STATE
// ============================================================

enum SystemState {
  STATE_NORMAL,
  STATE_WARNING,
  STATE_ACCIDENT
};

SystemState lastState = STATE_NORMAL;

// ============================================================
// GSM RECEIVE VARIABLES
// ============================================================

String gsmBuffer = "";
String smsHeader = "";
bool waitingBody = false;

// ============================================================
// EDGE IMPULSE FEATURES
// ============================================================

float features[9];

// ============================================================
// LED CONTROL
// ============================================================

void setRGB(bool r, bool g, bool b) {
  digitalWrite(LED_R, r);
  digitalWrite(LED_G, g);
  digitalWrite(LED_B, b);
}

// ------------------------------------------------------------
// Normal
// ------------------------------------------------------------

void normalState() {
  setRGB(0, 1, 0);
}

// ------------------------------------------------------------
// Warning
// ------------------------------------------------------------

void warningState() {
  setRGB(0, 0, 1);
}

// ------------------------------------------------------------
// Accident
// ------------------------------------------------------------

void accidentState() {
  setRGB(1, 0, 0);
}

// ============================================================
// SENSOR HELPERS
// ============================================================

float getSpeed() {
  return
    (analogRead(POT_PIN) / 4095.0)
    * 120.0;
}

// ------------------------------------------------------------
// Acceleration magnitude
// ------------------------------------------------------------

float accelMag(float x, float y, float z) {
  return sqrt(x * x + y * y + z * z);
}

// ------------------------------------------------------------
// Tilt
// ------------------------------------------------------------

float tiltCalc(float p, float r) {
  return sqrt(p * p + r * r);
}

// ============================================================
// GPS
// ============================================================

void updateGPS() {
  while (gpsSerial.available()) {
    gps.encode(gpsSerial.read());
  }
}

// ------------------------------------------------------------
// Return GPS as:
//
// latitude,longitude
//
// Example:
//
// 9.947192,78.818826
// ------------------------------------------------------------

String getLatLon() {
  updateGPS();
  if (gps.location.isValid()) {
    return
      String(gps.location.lat(), 6)
      + ","
      + String(gps.location.lng(), 6);
  }
  return "0.000000,0.000000";
}

// ============================================================
// GET GPS FOR LOC REQUEST
// ============================================================
//
// This function waits up to 30 seconds for a GPS fix.
//
// Return:
//
// Lat: 9.947192 | Lon: 78.818826
//
// OR
//
// GPS: UNAVAILABLE
// ============================================================

String getLatLonText() {
  Serial.println();
  Serial.println("[GPS] Getting bike location...");
  // ----------------------------------------------------------
  // Check current GPS data first
  // ----------------------------------------------------------
  updateGPS();
  if (gps.location.isValid()) {
    Serial.println("[GPS] Valid GPS fix available.");
    Serial.print("[GPS] Latitude: ");
    Serial.println(gps.location.lat(), 6);
    Serial.print("[GPS] Longitude: ");
    Serial.println(gps.location.lng(), 6);
    return
      "Lat: " + String(gps.location.lat(), 6)
      + " | Lon: " + String(gps.location.lng(), 6);
  }
  // ----------------------------------------------------------
  // No GPS fix
  // ----------------------------------------------------------
  Serial.println("[GPS] No valid fix.");
  Serial.println("[GPS] Waiting for GPS fix...");
  unsigned long startTime = millis();
  // ----------------------------------------------------------
  // Wait maximum 30 seconds
  // ----------------------------------------------------------
  while (millis() - startTime < 30000) {
    updateGPS();
    if (gps.location.isValid()) {
      Serial.println();
      Serial.println("[GPS] GPS FIX RECEIVED!");
      Serial.print("[GPS] Latitude: ");
      Serial.println(gps.location.lat(), 6);
      Serial.print("[GPS] Longitude: ");
      Serial.println(gps.location.lng(), 6);
      return
        "Lat: " + String(gps.location.lat(), 6)
        + " | Lon: " + String(gps.location.lng(), 6);
    }
    delay(10);
  }
  // ----------------------------------------------------------
  // GPS unavailable
  // ----------------------------------------------------------
  Serial.println();
  Serial.println("[GPS] ERROR: GPS fix unavailable.");
  return "GPS: UNAVAILABLE";
}

// ============================================================
// BLUETOOTH
// ============================================================

bool bluetoothConnected() {
  return SerialBT.hasClient();
}

// ============================================================
// SEND ACCIDENT PACKET TO ANDROID
// ============================================================

void sendBluetoothAccident(String severity, float speed) {
  bool gpsAvailable = false;
  updateGPS();
  if (gps.location.isValid()) {
    gpsAvailable = true;
  }
  String packet = "";
  packet += "AUTOGUARD";
  packet += "|ACCIDENT";
  packet += "|" + severity;
  packet += "|SPEED:" + String(speed, 0);
  if (gpsAvailable) {
    packet += "|GPS:YES";
    packet += "|LAT:" + String(gps.location.lat(), 6);
    packet += "|LON:" + String(gps.location.lng(), 6);
  }
  else {
    packet += "|GPS:NO";
    packet += "|LAT:0.000000";
    packet += "|LON:0.000000";
  }
  // ----------------------------------------------------------
  // IMPORTANT
  //
  // Android BluetoothManager expects newline.
  // ----------------------------------------------------------
  SerialBT.println(packet);
  Serial.println();
  Serial.println("========================================");
  Serial.println("BLUETOOTH ACCIDENT PACKET SENT");
  Serial.println("========================================");
  Serial.print("Packet: ");
  Serial.println(packet);
  Serial.println("========================================");
  Serial.println();
}

// ============================================================
// GSM INITIALIZATION
// ============================================================

void initializeGSM() {
  Serial.println();
  Serial.println("========================================");
  Serial.println("INITIALIZING SIM900A");
  Serial.println("========================================");
  // ----------------------------------------------------------
  // Give modem time to start
  // ----------------------------------------------------------
  delay(1000);
  // ----------------------------------------------------------
  // Test modem
  // ----------------------------------------------------------
  gsm.println("AT");
  delay(1000);
  // ----------------------------------------------------------
  // SMS TEXT MODE
  // ----------------------------------------------------------
  gsm.println("AT+CMGF=1");
  delay(500);
  // ----------------------------------------------------------
  // New SMS indication
  //
  // +CMT means:
  //
  // modem directly gives SMS header and body
  // ----------------------------------------------------------
  gsm.println("AT+CNMI=2,2,0,0,0");
  delay(500);
  // ----------------------------------------------------------
  // Character set
  // ----------------------------------------------------------
  gsm.println("AT+CSCS=\"GSM\"");
  delay(500);
  Serial.println("[GSM] SMS text mode enabled.");
  Serial.println("[GSM] Direct SMS notification enabled.");
  Serial.println("[GSM] SIM900A ready.");
  Serial.println("========================================");
  Serial.println();
}

// ============================================================
// GSM SEND SMS
// ============================================================
//
// Reliable SMS sending.
//
// Waits for:
//
// >
//
// before sending message.
// ============================================================

bool sendSMS(String num, String msg) {
  Serial.println();
  Serial.println("----------------------------------------");
  Serial.println("GSM SMS SEND");
  Serial.println("----------------------------------------");
  Serial.print("To: ");
  Serial.println(num);
  Serial.print("Message: ");
  Serial.println(msg);
  if (num.length() == 0) {
    Serial.println("[GSM] ERROR: Empty phone number.");
    return false;
  }
  // ----------------------------------------------------------
  // Clear old modem data
  // ----------------------------------------------------------
  while (gsm.available()) {
    gsm.read();
  }
  // ----------------------------------------------------------
  // SMS text mode
  // ----------------------------------------------------------
  gsm.println("AT+CMGF=1");
  delay(500);
  // ----------------------------------------------------------
  // Start SMS
  // ----------------------------------------------------------
  gsm.print("AT+CMGS=\"");
  gsm.print(num);
  gsm.println("\"");
  // ----------------------------------------------------------
  // Wait for >
  // ----------------------------------------------------------
  unsigned long start = millis();
  bool promptReceived = false;
  while (millis() - start < 10000) {
    if (gsm.available()) {
      char c = gsm.read();
      Serial.write(c);
      if (c == '>') {
        promptReceived = true;
        break;
      }
    }
  }
  if (!promptReceived) {
    Serial.println();
    Serial.println("[GSM] ERROR: No SMS prompt received.");
    Serial.println("[GSM] SMS was NOT sent.");
    return false;
  }
  // ----------------------------------------------------------
  // Send message
  // ----------------------------------------------------------
  gsm.print(msg);
  // ----------------------------------------------------------
  // CTRL+Z
  // ----------------------------------------------------------
  gsm.write(26);
  // ----------------------------------------------------------
  // Wait for modem result
  // ----------------------------------------------------------
  start = millis();
  String response = "";
  while (millis() - start < 15000) {
    while (gsm.available()) {
      char c = gsm.read();
      Serial.write(c);
      response += c;
    }
    if (response.indexOf("OK") >= 0) {
      Serial.println();
      Serial.println("[GSM] SMS SENT SUCCESSFULLY.");
      Serial.println("----------------------------------------");
      return true;
    }
    if (response.indexOf("ERROR") >= 0) {
      Serial.println();
      Serial.println("[GSM] ERROR: SMS sending failed.");
      Serial.println("----------------------------------------");
      return false;
    }
    delay(20);
  }
  Serial.println();
  Serial.println("[GSM] SMS send timeout.");
  Serial.println("----------------------------------------");
  return false;
}

// ============================================================
// SEND GSM ALERT TO BOTH NUMBERS
// ============================================================

void sendAll(String msg) {
  Serial.println();
  Serial.println("==== GSM ALERT BROADCAST ====");
  sendSMS(phone1, msg);
  delay(1000);
  sendSMS(phone2, msg);
  Serial.println("==== GSM BROADCAST COMPLETE ====");
  Serial.println();
}

// ============================================================
// ACCIDENT GSM ALERT
// ============================================================

void sendModerate(float speed) {
  String msg = "MODERATE ACCIDENT | Speed: "
    + String(speed, 0)
    + " | "
    + getLatLonText();
  Serial.println("Severity decision: MODERATE");
  sendAll(msg);
}

// ============================================================

void sendSevere(float speed) {
  String msg = "SEVERE ACCIDENT | Speed: "
    + String(speed, 0)
    + " | Location: "
    + getLatLon();
  Serial.println("Severity decision: SEVERE");
  sendAll(msg);
}

// ============================================================

void sendUnknown(float speed) {
  String msg = "ACCIDENT DETECTED (severity unknown) | Speed: "
    + String(speed, 0)
    + " | "
    + getLatLonText();
  Serial.println("Severity decision: UNKNOWN");
  sendAll(msg);
}

// ============================================================
// OPTION A ACCIDENT ROUTER
// ============================================================

void sendAccidentAlert(String severity, float speed) {
  Serial.println();
  Serial.println("########################################");
  Serial.println("        ACCIDENT ALERT ROUTER");
  Serial.println("########################################");
  // ----------------------------------------------------------
  // Bluetooth connected
  // ----------------------------------------------------------
  if (bluetoothConnected()) {
    Serial.println("Bluetooth: CONNECTED");
    Serial.println("Route: ESP32 -> Android App");
    Serial.println("GSM accident SMS: DISABLED");
    sendBluetoothAccident(severity, speed);
  }
  // ----------------------------------------------------------
  // Bluetooth disconnected
  // ----------------------------------------------------------
  else {
    Serial.println("Bluetooth: DISCONNECTED");
    Serial.println("Route: ESP32 -> GSM");
    if (severity == "MODERATE") {
      sendModerate(speed);
    }
    else if (severity == "SEVERE") {
      sendSevere(speed);
    }
    else {
      sendUnknown(speed);
    }
  }
  Serial.println("########################################");
  Serial.println();
}

// ============================================================
// EXTRACT SMS SENDER NUMBER
// ============================================================
//
// SIM900A example:
//
// +CMT: "+918012776655","","26/08/28,10:30:00+22"
//
// The FIRST quoted field is the sender.
//
// IMPORTANT:
// The old code was extracting the third quoted field,
// which is wrong.
//
// ============================================================

String extractNumber(String header) {
  int firstQuote = header.indexOf('"');
  if (firstQuote < 0) {
    return "";
  }
  int secondQuote = header.indexOf('"', firstQuote + 1);
  if (secondQuote < 0) {
    return "";
  }
  return header.substring(firstQuote + 1, secondQuote);
}

// ============================================================
// NORMALIZE PHONE NUMBER
// ============================================================

String normalizePhoneNumber(String number) {
  number.trim();
  number.replace(" ", "");
  number.replace("-", "");
  return number;
}

// ============================================================
// HANDLE INCOMING SMS
// ============================================================

void handleSMS(String body) {
  body.trim();
  Serial.print("[SMS] Received: ");
  Serial.println(body);
  if (body.equalsIgnoreCase("LOC")) {
    String sender = extractNumber(smsHeader);
    Serial.println("================================");
    Serial.println("LOC COMMAND RECEIVED");
    Serial.print("Request sender: ");
    Serial.println(sender);
    Serial.println("================================");
    if (sender.length() == 0) {
      Serial.println("[LOC ERROR] Sender number not found.");
      return;
    }
    String location = getLatLon();
    Serial.print("[LOC] GPS: ");
    Serial.println(location);
    if (location == "0.000000,0.000000") {
      Serial.println("[LOC ERROR] GPS location unavailable.");
      String msg = "Bike Location | Lat: 0.000000 | Lon: 0.000000";
      sendSMS(sender, msg);
      return;
    }
    int comma = location.indexOf(',');
    String lat = location.substring(0, comma);
    String lon = location.substring(comma + 1);
    String msg = "Bike Location | Lat: " + lat + " | Lon: " + lon;
    Serial.println("[LOC] Sending response:");
    Serial.println(msg);
    sendSMS(sender, msg);
    Serial.println("[LOC] Response SMS sent.");
  }
  else {
    Serial.println("[SMS] Not a LOC command.");
  }
}

// ============================================================
// READ GSM
// ============================================================
//
// Expected SIM900A:
//
// +CMT: "+918012776655","","26/08/28,10:30:00+22"
// LOC
//
// ============================================================

void readGSM() {
  while (gsm.available()) {
    char c = gsm.read();
    // IMPORTANT:
    // Show EVERYTHING received from GSM on Serial Monitor
    Serial.write(c);
    if (c == '\n' || c == '\r') {
      if (gsmBuffer.length() > 0) {
        gsmBuffer.trim();
        Serial.println();
        Serial.print("[GSM LINE] ");
        Serial.println(gsmBuffer);
        if (gsmBuffer.startsWith("+CMT:")) {
          smsHeader = gsmBuffer;
          waitingBody = true;
          Serial.println("[LOC] SMS HEADER DETECTED");
          Serial.print("[LOC] Sender: ");
          Serial.println(extractNumber(smsHeader));
        }
        else if (waitingBody) {
          Serial.print("[LOC] SMS BODY: ");
          Serial.println(gsmBuffer);
          handleSMS(gsmBuffer);
          waitingBody = false;
        }
        gsmBuffer = "";
      }
    }
    else {
      gsmBuffer += c;
      // Prevent String from growing forever
      if (gsmBuffer.length() > 300) {
        gsmBuffer = "";
        waitingBody = false;
      }
    }
  }
}

// ============================================================
// EDGE IMPULSE DATA CALLBACK
// ============================================================

static int get_data(size_t offset, size_t length, float *out_ptr) {
  for (size_t i = 0; i < length; i++) {
    out_ptr[i] = features[offset + i];
  }
  return 0;
}

// ============================================================
// RUN EDGE IMPULSE MODEL
// ============================================================

bool runML(String &outLabel, float &outConfidence) {
  signal_t signal;
  signal.total_length = 9;
  signal.get_data = get_data;
  ei_impulse_result_t result;
  EI_IMPULSE_ERROR res = run_classifier(&signal, &result, false);
  if (res != EI_IMPULSE_OK) {
    Serial.print("ML ERROR: ");
    Serial.println(res);
    return false;
  }
  float best = 0;
  String label = "";
  Serial.println();
  Serial.println("Predictions:");
  for (int i = 0; i < EI_CLASSIFIER_LABEL_COUNT; i++) {
    Serial.print(result.classification[i].label);
    Serial.print(": ");
    Serial.println(result.classification[i].value);
    if (result.classification[i].value > best) {
      best = result.classification[i].value;
      label = result.classification[i].label;
    }
  }
  outLabel = label;
  outConfidence = best;
  return
    label.length()
    >
    0;
}

// ============================================================
// SETUP
// ============================================================

void setup() {
  // ----------------------------------------------------------
  // Serial Monitor
  // ----------------------------------------------------------
  Serial.begin(115200);
  delay(1000);
  Serial.println();
  Serial.println("========================================");
  Serial.println("       AUTO GUARD ESP32 STARTING");
  Serial.println("========================================");
  // ==========================================================
  // PINS
  // ==========================================================
  pinMode(POT_PIN, INPUT);
  pinMode(BUTTON_PIN, INPUT_PULLUP);
  pinMode(BUZZER, OUTPUT);
  pinMode(LED_R, OUTPUT);
  pinMode(LED_G, OUTPUT);
  pinMode(LED_B, OUTPUT);
  digitalWrite(BUZZER, LOW);
  Serial.println("[OK] Pins configured.");
  // ==========================================================
  // I2C
  // ==========================================================
  Wire.begin(SDA_PIN, SCL_PIN);
  Serial.println("[OK] I2C started.");
  Serial.print("SDA: ");
  Serial.println(SDA_PIN);
  Serial.print("SCL: ");
  Serial.println(SCL_PIN);
  // ==========================================================
  // MPU9250
  // ==========================================================
  mpu.setWire(&Wire);
  mpu.beginAccel();
  mpu.beginGyro();
  Serial.println("[OK] MPU9250 initialized.");
  // ==========================================================
  // GPS
  // ==========================================================
  gpsSerial.begin(9600, SERIAL_8N1, GPS_RX, GPS_TX);
  Serial.println("[OK] NEO-6M GPS started.");
  Serial.print("GPS RX: ");
  Serial.println(GPS_RX);
  Serial.print("GPS TX: ");
  Serial.println(GPS_TX);
  // ==========================================================
  // GSM
  // ==========================================================
  gsm.begin(9600, SERIAL_8N1, GSM_RX, GSM_TX);
  delay(1000);
  Serial.println("===== Initializing GSM SMS reception =====");
  gsm.println("AT");
  delay(1000);
  gsm.println("ATE0");
  delay(500);
  gsm.println("AT+CMGF=1");
  delay(500);
  gsm.println("AT+CNMI=2,2,0,0,0");
  delay(500);
  gsm.println("AT+CSCS=\"GSM\"");
  delay(500);
  Serial.println("===== GSM SMS reception configured =====");
  Serial.println("[OK] SIM900A serial started.");
  Serial.print("GSM RX: ");
  Serial.println(GSM_RX);
  Serial.print("GSM TX: ");
  Serial.println(GSM_TX);
  // ----------------------------------------------------------
  // IMPORTANT
  // Configure SMS reception
  // ----------------------------------------------------------
  initializeGSM();
  // ==========================================================
  // BLUETOOTH
  // ==========================================================
  Serial.println();
  Serial.println("Starting Classic Bluetooth...");
  if (!SerialBT.begin(BT_DEVICE_NAME)) {
    Serial.println("[ERROR] Bluetooth initialization failed.");
  }
  else {
    Serial.println("[OK] Bluetooth initialized.");
    Serial.print("Bluetooth name: ");
    Serial.println(BT_DEVICE_NAME);
    Serial.println("Bluetooth mode: Classic SPP");
    Serial.println("SPP UUID: 00001101-0000-1000-8000-00805F9B34FB");
  }
  // ==========================================================
  // CONFIGURATION DISPLAY
  // ==========================================================
  Serial.println();
  Serial.print("GSM fallback number 1: ");
  Serial.println(phone1);
  Serial.print("GSM fallback number 2: ");
  Serial.println(phone2);
  Serial.print("Warning speed: ");
  Serial.println(WARNING_SPEED);
  Serial.print("Warning tilt: ");
  Serial.println(WARNING_TILT);
  Serial.print("Accident tilt: ");
  Serial.println(ACCIDENT_TILT);
  Serial.print("ML minimum confidence: ");
  Serial.println(ML_MIN_CONFIDENCE);
  // ==========================================================
  // INITIAL STATE
  // ==========================================================
  normalState();
  Serial.println();
  Serial.println("========================================");
  Serial.println("          AUTO GUARD READY");
  Serial.println("========================================");
  Serial.println("Bluetooth name:");
  Serial.println("AUTO_GUARD");
  Serial.println();
  Serial.println("ACCIDENT:");
  Serial.println("ESP32 -> Bluetooth -> Android -> SMS");
  Serial.println();
  Serial.println("LOC:");
  Serial.println("Android -> GSM -> ESP32 -> GPS -> GSM -> Android");
  Serial.println();
  Serial.println("========================================");
}

// ============================================================
// LOOP
// ============================================================

void loop() {
  // ==========================================================
  // GSM RECEIVE
  // ==========================================================
  readGSM();
  // ==========================================================
  // GPS UPDATE
  // ==========================================================
  updateGPS();
  // ==========================================================
  // MPU UPDATE
  // ==========================================================
  mpu.accelUpdate();
  mpu.gyroUpdate();
  float ax = mpu.accelX();
  float ay = mpu.accelY();
  float az = mpu.accelZ();
  // ==========================================================
  // ACCELERATION
  // ==========================================================
  float accel = accelMag(ax, ay, az);
  // ==========================================================
  // TILT
  // ==========================================================
  float pitch = atan2(-ay, sqrt(ax * ax + az * az))
    * 180.0 / PI;
  float roll = atan2(ax, -az)
    * 180.0 / PI;
  float tilt = tiltCalc(pitch, roll);
  // ==========================================================
  // SPEED
  // ==========================================================
  float speed = getSpeed();
  // ==========================================================
  // DIFFERENCES
  // ==========================================================
  float dP = pitch - prevPitch;
  float dR = roll - prevRoll;
  float dS = speed - prevSpeed;
  // ==========================================================
  // IMPACT
  // ==========================================================
  int impact = 0;
  if (accel > 2.0 || abs(dS) > 30) {
    impact = 1;
  }
  // ==========================================================
  // WARNING
  // ==========================================================
  bool warning = (speed > WARNING_SPEED || tilt > WARNING_TILT);
  // ==========================================================
  // ACCIDENT
  // ==========================================================
  bool accident = ((speed > MIN_SPEED && tilt > ACCIDENT_TILT) || (impact == 1 && tilt > WARNING_TILT));
  // ==========================================================
  // SERIAL SENSOR LOG
  // ==========================================================
  if (millis() - lastLoopLog >= LOOP_LOG_INTERVAL) {
    lastLoopLog = millis();
    Serial.print("Speed: ");
    Serial.print(speed, 1);
    Serial.print(" km/h | Tilt: ");
    Serial.print(tilt, 1);
    Serial.print(" deg | Accel: ");
    Serial.print(accel, 2);
    Serial.print(" | Impact: ");
    Serial.print(impact);
    Serial.print(" | GPS: ");
    Serial.print(gps.location.isValid() ? "YES" : "NO");
    Serial.print(" | BT: ");
    Serial.println(bluetoothConnected() ? "CONNECTED" : "DISCONNECTED");
  }
  // ==========================================================
  // NORMAL / WARNING
  // ==========================================================
  if (!accident) {
    if (warning) {
      warningState();
      if (lastState != STATE_WARNING) {
        Serial.println(">>> State: WARNING");
        lastState = STATE_WARNING;
      }
    }
    else {
      normalState();
      if (lastState != STATE_NORMAL) {
        Serial.println(">>> State: NORMAL");
        lastState = STATE_NORMAL;
      }
    }
  }
  // ==========================================================
  // ACCIDENT
  // ==========================================================
  bool cancelled = false;
  if (accident) {
    if (lastState != STATE_ACCIDENT) {
      Serial.println();
      Serial.println("########################################");
      Serial.println("          ACCIDENT DETECTED");
      Serial.println("########################################");
      Serial.println("Starting 10 second cancel window...");
      lastState = STATE_ACCIDENT;
    }
    // --------------------------------------------------------
    // RED LED + BUZZER
    // --------------------------------------------------------
    accidentState();
    digitalWrite(BUZZER, HIGH);
    // --------------------------------------------------------
    // CANCEL WINDOW
    // --------------------------------------------------------
    unsigned long t = millis();
    while (millis() - t < CANCEL_TIME) {
      // Keep GPS active
      updateGPS();
      // Keep GSM active
      readGSM();
      // ------------------------------------------------------
      // Cancel button
      // ------------------------------------------------------
      if (digitalRead(BUTTON_PIN) == LOW) {
        digitalWrite(BUZZER, LOW);
        normalState();
        cancelled = true;
        Serial.println();
        Serial.println(">>> Accident CANCELLED.");
        lastState = STATE_NORMAL;
        break;
      }
      delay(10);
    }
    // ========================================================
    // CANCELLED
    // ========================================================
    if (cancelled) {
      // Nothing else
    }
    // ========================================================
    // SEND ALERT
    // ========================================================
    else {
      digitalWrite(BUZZER, LOW);
      Serial.println();
      Serial.println(">>> Cancel window expired.");
      Serial.println(">>> Running ML classification...");
      // ------------------------------------------------------
      // ML FEATURES
      // ------------------------------------------------------
      features[0] = speed;
      features[1] = pitch;
      features[2] = roll;
      features[3] = accel;
      features[4] = tilt;
      features[5] = dP;
      features[6] = dR;
      features[7] = dS;
      features[8] = impact;
      // ------------------------------------------------------
      // RUN ML
      // ------------------------------------------------------
      String label = "";
      float confidence = 0;
      bool ok = runML(label, confidence);
      // ------------------------------------------------------
      // DEBUG
      // ------------------------------------------------------
      Serial.println();
      Serial.println("========== ACCIDENT DATA ==========");
      Serial.print("Pitch: ");
      Serial.println(pitch);
      Serial.print("Roll: ");
      Serial.println(roll);
      Serial.print("Tilt: ");
      Serial.println(tilt);
      Serial.print("Speed: ");
      Serial.println(speed);
      Serial.print("Acceleration: ");
      Serial.println(accel);
      Serial.print("Impact: ");
      Serial.println(impact);
      Serial.print("GPS: ");
      Serial.println(gps.location.isValid() ? "YES" : "NO");
      if (gps.location.isValid()) {
        Serial.print("Latitude: ");
        Serial.println(gps.location.lat(), 6);
        Serial.print("Longitude: ");
        Serial.println(gps.location.lng(), 6);
      }
      Serial.print("ML label: ");
      Serial.println(label);
      Serial.print("ML confidence: ");
      Serial.println(confidence);
      Serial.println("====================================");
      // ------------------------------------------------------
      // SEVERITY
      // ------------------------------------------------------
      String severity = "UNKNOWN";
      if (ok && confidence >= ML_MIN_CONFIDENCE) {
        if (label.indexOf("Moderate") >= 0) {
          severity = "MODERATE";
        }
        else if (label.indexOf("Severe") >= 0) {
          severity = "SEVERE";
        }
      }
      Serial.print("FINAL SEVERITY: ");
      Serial.println(severity);
      // ------------------------------------------------------
      // OPTION A ROUTER
      // ------------------------------------------------------
      sendAccidentAlert(severity, speed);
      // ------------------------------------------------------
      // RESET
      // ------------------------------------------------------
      delay(3000);
      normalState();
      lastState = STATE_NORMAL;
      Serial.println(">>> State changed to NORMAL.");
    }
  }
  // ==========================================================
  // UPDATE HISTORY
  // ==========================================================
  prevPitch = pitch;
  prevRoll = roll;
  prevAccel = accel;
  prevSpeed =speed;
  // ==========================================================
  // LOOP DELAY
  // ==========================================================
  delay(200);
}