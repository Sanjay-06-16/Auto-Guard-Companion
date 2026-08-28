package com.autoguard.companion.sms

import android.util.Log

data class ParsedSms(
    val type: String, // "ACCIDENT" or "LOCATION"
    val severity: String, // "MODERATE", "SEVERE", "MINOR", "LOCATION", or "N/A"
    val speed: Int?,
    val lat: Double?,
    val lon: Double?
)

object SmsParser {
    
    private const val TAG = "AutoGuardSms"

    /**
     * Robustly parses incoming SMS messages from the ESP32.
     * Supported formats:
     * 1. “MODERATE ACCIDENT | Speed: 54 | Lat: 13.0827 | Lon: 80.2707”
     * 2. “Bike Location | Lat: 9.947192 | Lon: 78.818826”
     */
    fun parseMessage(message: String): ParsedSms? {
        val upperMsg = message.uppercase()
        
        val type = when {
            upperMsg.contains("ACCIDENT") -> "ACCIDENT"
            upperMsg.contains("BIKE LOCATION") -> "LOCATION"
            else -> {
                Log.d(TAG, "Message type unknown or irrelevant: $message")
                return null
            }
        }
        
        var severity = "N/A"
        if (type == "ACCIDENT") {
            severity = when {
                upperMsg.contains("SEVERE") -> "SEVERE"
                upperMsg.contains("MODERATE") -> "MODERATE"
                upperMsg.contains("MINOR") -> "MINOR"
                else -> "UNKNOWN"
            }
        } else if (type == "LOCATION") {
            severity = "LOCATION"
        }
        
        val speedRegex = Regex("SPEED\\s*:\\s*(\\d+)", RegexOption.IGNORE_CASE)
        val speed = speedRegex.find(message)?.groupValues?.get(1)?.toIntOrNull()
        
        // Robust extraction using independent regex for Lat and Lon with flexible spacing
        val latRegex = Regex("LAT\\s*:\\s*([0-9.-]+)", RegexOption.IGNORE_CASE)
        val lonRegex = Regex("LON\\s*:\\s*([0-9.-]+)", RegexOption.IGNORE_CASE)
        
        val latMatch = latRegex.find(message)
        val lonMatch = lonRegex.find(message)
        
        val (lat, lon) = if (latMatch != null && lonMatch != null) {
            latMatch.groupValues[1].toDoubleOrNull() to lonMatch.groupValues[1].toDoubleOrNull()
        } else {
            // Fallback for combined format: Location: 13.0827,80.2707
            val locRegex = Regex("LOCATION\\s*:\\s*([0-9.-]+)\\s*[,|]\\s*([0-9.-]+)", RegexOption.IGNORE_CASE)
            locRegex.find(message)?.let {
                it.groupValues[1].toDoubleOrNull() to it.groupValues[2].toDoubleOrNull()
            } ?: (null to null)
        }

        // Coordinate Validation
        if (lat != null && lon != null) {
            if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
                Log.e(TAG, "Invalid coordinates parsed: Lat $lat, Lon $lon")
                return null
            }
            // Logs for latitude and longitude will be handled in SmsReceiver to match user request list
        } else if (type == "LOCATION") {
            Log.e(TAG, "Failed to parse coordinates for LOCATION message: $message")
            return null
        }
        
        return ParsedSms(type, severity, speed, lat, lon)
    }
}
