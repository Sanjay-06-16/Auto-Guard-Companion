package com.autoguard.companion.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autoguard.companion.bluetooth.BluetoothManager
import com.autoguard.companion.bluetooth.BluetoothState
import com.autoguard.companion.data.AppDatabase
import com.autoguard.companion.data.entity.AlertEntity
import com.autoguard.companion.data.entity.ContactEntity
import com.autoguard.companion.data.entity.ProfileEntity
import com.autoguard.companion.location.LocationHelper
import com.autoguard.companion.repository.AppRepository
import com.autoguard.companion.sms.SmsSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.util.Log

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository
    private val smsSender = SmsSender()
    private val bluetoothManager = BluetoothManager(application)
    private val locationHelper = LocationHelper(application)
    
    val bluetoothState: StateFlow<BluetoothState> = bluetoothManager.connectionState
    val bluetoothErrorMessage: StateFlow<String?> = bluetoothManager.errorMessage
    val esp32GpsAvailable: StateFlow<Boolean> = bluetoothManager.esp32GpsAvailable

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.alertDao(), database.profileDao(), database.contactDao())
        
        bluetoothManager.setAccidentListener { event ->
            handleAccidentEvent(event)
        }
    }

    val alerts: StateFlow<List<AlertEntity>> = repository.getAllAlerts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        
    val profile: StateFlow<ProfileEntity?> = repository.getProfile()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
        
    val contacts: StateFlow<List<ContactEntity>> = repository.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        
    val latestLocation: StateFlow<AlertEntity?> = repository.getLatestLocation()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun connectBluetooth() {
        bluetoothManager.connectToDevice()
    }

    fun disconnectBluetooth() {
        bluetoothManager.disconnect()
    }

    private fun handleAccidentEvent(
        event: com.autoguard.companion.bluetooth.AccidentEvent
    ) {
        viewModelScope.launch {

            var finalLat: Double? = null
            var finalLon: Double? = null
            var locationSource = "NONE"

            Log.d("AutoGuardLocation", "========== ACCIDENT EVENT ==========")
            Log.d("AutoGuardLocation", "Severity: ${event.severity}")
            Log.d("AutoGuardLocation", "Speed: ${event.speed}")
            Log.d("AutoGuardLocation", "ESP32 GPS Available: ${event.esp32GpsAvailable}")
            Log.d("AutoGuardLocation", "ESP32 Lat: ${event.latitude}")
            Log.d("AutoGuardLocation", "ESP32 Lon: ${event.longitude}")

            // =========================================================
            // 1. TRY ESP32 GPS FIRST
            // =========================================================
            if (
                event.esp32GpsAvailable &&
                event.latitude != 0.0 &&
                event.longitude != 0.0
            ) {

                finalLat = event.latitude
                finalLon = event.longitude
                locationSource = "ESP32"

                Log.d(
                    "AutoGuardLocation",
                    "Using ESP32 GPS: $finalLat, $finalLon"
                )

            } else {

                // =====================================================
                // 2. ESP32 GPS NOT AVAILABLE → TRY PHONE GPS
                // =====================================================

                Log.d(
                    "AutoGuardLocation",
                    "ESP32 GPS unavailable. Requesting phone GPS..."
                )

                val phoneLocation = locationHelper.getCurrentLocation()

                if (phoneLocation != null) {

                    finalLat = phoneLocation.latitude
                    finalLon = phoneLocation.longitude
                    locationSource = "PHONE"

                    Log.d(
                        "AutoGuardLocation",
                        "Using phone GPS: $finalLat, $finalLon"
                    )

                } else {

                    Log.e(
                        "AutoGuardLocation",
                        "Phone GPS also unavailable"
                    )

                    locationSource = "NONE"
                }
            }

            // =========================================================
            // 3. NEVER ACCEPT 0,0
            // =========================================================

            if (
                finalLat == null ||
                finalLon == null ||
                (finalLat == 0.0 && finalLon == 0.0)
            ) {

                finalLat = null
                finalLon = null

                if (locationSource != "ESP32" && locationSource != "PHONE") {
                    locationSource = "NONE"
                }

                Log.e(
                    "AutoGuardLocation",
                    "FINAL LOCATION = UNAVAILABLE"
                )

            } else {

                Log.d(
                    "AutoGuardLocation",
                    "FINAL LOCATION = $finalLat, $finalLon"
                )
                Log.d(
                    "AutoGuardLocation",
                    "LOCATION SOURCE = $locationSource"
                )
            }

            // =========================================================
            // 4. GET RIDER PROFILE
            // =========================================================

            val currentProfile = profile.value

            // =========================================================
            // 5. GET EMERGENCY CONTACTS
            // =========================================================

            val emergencyContacts = mutableListOf<String>()

            currentProfile?.let {

                if (it.emergencyContact1.isNotBlank()) {
                    emergencyContacts.add(it.emergencyContact1.trim())
                }

                if (it.emergencyContact2.isNotBlank()) {
                    emergencyContacts.add(it.emergencyContact2.trim())
                }
            }

            Log.d(
                "AutoGuardLocation",
                "Emergency contacts: ${emergencyContacts.size}"
            )

            // =========================================================
            // 6. SEND SMS
            // =========================================================

            var smsStatus = "NOT_SENT"

            if (emergencyContacts.isNotEmpty()) {

                var allSent = true

                emergencyContacts.forEach { phoneNumber ->

                    Log.d(
                        "AutoGuardLocation",
                        "Sending emergency SMS to: $phoneNumber"
                    )

                    val sent = smsSender.sendEmergencySms(
                        phoneNumber = phoneNumber,
                        severity = event.severity,
                        speed = event.speed,
                        latitude = finalLat,
                        longitude = finalLon,
                        locationSource = locationSource,
                        profile = currentProfile
                    )

                    if (!sent) {
                        allSent = false

                        Log.e(
                            "AutoGuardLocation",
                            "SMS FAILED: $phoneNumber"
                        )

                    } else {

                        Log.d(
                            "AutoGuardLocation",
                            "SMS SENT: $phoneNumber"
                        )
                    }
                }

                smsStatus = if (allSent) {
                    "SENT"
                } else {
                    "FAILED"
                }

            } else {

                smsStatus = "NO_CONTACTS"

                Log.e(
                    "AutoGuardLocation",
                    "No emergency contacts configured"
                )
            }

            // =========================================================
            // 7. SAVE ALERT TO ROOM
            // =========================================================

            repository.insertAlert(
                AlertEntity(
                    type = "ACCIDENT",
                    severity = event.severity,
                    speed = event.speed,
                    latitude = finalLat,
                    longitude = finalLon,
                    timestamp = event.timestamp,

                    rawMessage =
                        if (emergencyContacts.isEmpty()) {
                            "No emergency contacts saved. Please add emergency contacts in Rider Profile."
                        } else {
                            "Accident Alert Processed via Bluetooth"
                        },

                    locationSource = locationSource,
                    smsStatus = smsStatus
                )
            )

            Log.d(
                "AutoGuardLocation",
                "========== ACCIDENT PROCESSING COMPLETE =========="
            )
            Log.d(
                "AutoGuardLocation",
                "Location Source: $locationSource"
            )
            Log.d(
                "AutoGuardLocation",
                "Latitude: $finalLat"
            )
            Log.d(
                "AutoGuardLocation",
                "Longitude: $finalLon"
            )
            Log.d(
                "AutoGuardLocation",
                "SMS Status: $smsStatus"
            )
        }
    }
    fun sendLocationRequest() {
        viewModelScope.launch {
            val currentProfile = profile.value
            if (currentProfile != null && currentProfile.bikeGsmNumber.isNotBlank()) {
                smsSender.sendLocationRequest(currentProfile.bikeGsmNumber)
                
                repository.insertAlert(
                    AlertEntity(
                        type = "LOCATION_REQUEST",
                        severity = "INFO",
                        speed = null,
                        latitude = null,
                        longitude = null,
                        timestamp = System.currentTimeMillis(),
                        rawMessage = "Requested location update"
                    )
                )
            }
        }
    }

    fun saveProfile(
        name: String, 
        bloodGroup: String, 
        bikeNumber: String, 
        bikeGsmNumber: String, 
        riderPhoneNumber: String,
        emergencyContact1: String,
        emergencyContact2: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.insertOrUpdateProfile(
                ProfileEntity(
                    id = 1,
                    name = name,
                    bloodGroup = bloodGroup,
                    bikeNumber = bikeNumber,
                    bikeGsmNumber = bikeGsmNumber,
                    riderPhoneNumber = riderPhoneNumber,
                    emergencyContact1 = emergencyContact1,
                    emergencyContact2 = emergencyContact2,
                    medicalNotes = notes
                )
            )
        }
    }

    fun addContact(name: String, phoneNumber: String) {
        viewModelScope.launch {
            repository.insertContact(ContactEntity(name = name, phoneNumber = phoneNumber))
        }
    }

    fun deleteContact(contact: ContactEntity) {
        viewModelScope.launch {
            repository.deleteContact(contact)
        }
    }
    
    fun markAlertAsViewed(alert: AlertEntity) {
        viewModelScope.launch {
            repository.updateAlert(alert.copy(isViewed = true))
        }
    }

    suspend fun getAlertById(id: Int): AlertEntity? {
        return repository.getAlertById(id)
    }
}
