package com.autoguard.companion.sms

import android.telephony.SmsManager
import android.util.Log
import com.autoguard.companion.data.entity.ProfileEntity

class SmsSender {

    fun sendLocationRequest(phoneNumber: String) {

        if (phoneNumber.isBlank()) return

        try {

            val smsManager = SmsManager.getDefault()

            smsManager.sendTextMessage(
                phoneNumber,
                null,
                "LOC",
                null,
                null
            )

            Log.d(
                "SmsSender",
                "LOC request sent to $phoneNumber"
            )

        } catch (e: Exception) {

            Log.e(
                "SmsSender",
                "Failed to send LOC request",
                e
            )
        }
    }

    fun sendEmergencySms(
        phoneNumber: String,
        severity: String,
        speed: Int,
        latitude: Double?,
        longitude: Double?,
        locationSource: String,
        profile: ProfileEntity?
    ): Boolean {

        if (phoneNumber.isBlank()) {
            return false
        }

        val riderName =
            profile?.name?.takeIf { it.isNotBlank() }
                ?: "Unknown"

        val bloodGroup =
            profile?.bloodGroup?.takeIf { it.isNotBlank() }
                ?: "Unknown"

        val bikeNumber =
            profile?.bikeNumber?.takeIf { it.isNotBlank() }
                ?: "Unknown"

        val medicalNotes =
            profile?.medicalNotes?.takeIf { it.isNotBlank() }
                ?: "Not available"

        val locationSection =
            if (
                latitude != null &&
                longitude != null &&
                latitude != 0.0 &&
                longitude != 0.0
            ) {

                """
                Location:
                Latitude: ${"%.6f".format(latitude)}
                Longitude: ${"%.6f".format(longitude)}
                
                Google Maps:
                https://maps.google.com/?q=$latitude,$longitude
                """.trimIndent()

            } else {

                "Location: Unknown"
            }

        val message =
            """
            AUTO GUARD EMERGENCY ALERT
            
            ${severity.uppercase()} ACCIDENT DETECTED
            
            Rider: $riderName
            Bike: $bikeNumber
            Blood Group: $bloodGroup
            Speed: $speed km/h
            
            Location Source: $locationSource
            
            $locationSection
            
            Medical Notes:
            $medicalNotes
            
            Please respond immediately.
            """.trimIndent()

        return try {

            val smsManager =
                SmsManager.getDefault()

            val parts =
                smsManager.divideMessage(message)

            smsManager.sendMultipartTextMessage(
                phoneNumber,
                null,
                parts,
                null,
                null
            )

            Log.d(
                "SmsSender",
                "Emergency SMS sent to $phoneNumber"
            )

            Log.d(
                "SmsSender",
                "Location = $latitude,$longitude"
            )

            true

        } catch (e: Exception) {

            Log.e(
                "SmsSender",
                "Failed to send emergency SMS to $phoneNumber",
                e
            )

            false
        }
    }
}