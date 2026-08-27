package com.autoguard.companion.sms

import android.util.Log

data class ParsedSms(
    val type: String, // "ACCIDENT" or "LOCATION"
    val severity: String, // "MODERATE", "SEVERE", "MINOR", or "N/A"
    val speed: Int?,
    val lat: Double?,
    val lon: Double?
)

object SmsParser {
    
    // Formats:
    // 1. “MODERATE ACCIDENT | Speed: 54 | Lat: 13.0827 | Lon: 80.2707”
    // 2. “SEVERE ACCIDENT | Speed: 92 | Location: 13.0827,80.2707”
    // 3. “Bike Location | Lat: 13.0827 | Lon: 80.2707”
    
    fun parseMessage(message: String): ParsedSms? {
        val upperMsg = message.uppercase()
        
        val type = if (upperMsg.contains("ACCIDENT")) "ACCIDENT" else if (upperMsg.contains("BIKE LOCATION")) "LOCATION" else return null
        
        var severity = "N/A"
        if (type == "ACCIDENT") {
            if (upperMsg.contains("SEVERE")) severity = "SEVERE"
            else if (upperMsg.contains("MODERATE")) severity = "MODERATE"
            else if (upperMsg.contains("MINOR")) severity = "MINOR"
        }
        
        var speed: Int? = null
        val speedRegex = Regex("SPEED:\\s*(\\d+)", RegexOption.IGNORE_CASE)
        val speedMatch = speedRegex.find(message)
        if (speedMatch != null) {
            speed = speedMatch.groupValues[1].toIntOrNull()
        }
        
        var lat: Double? = null
        var lon: Double? = null
        
        // Try format with separate Lat and Lon: Lat: 13.0827 | Lon: 80.2707
        val latLonRegex = Regex("LAT:\\s*([0-9.-]+).*LON:\\s*([0-9.-]+)", RegexOption.IGNORE_CASE)
        val latLonMatch = latLonRegex.find(message)
        if (latLonMatch != null) {
            lat = latLonMatch.groupValues[1].toDoubleOrNull()
            lon = latLonMatch.groupValues[2].toDoubleOrNull()
        } else {
            // Try combined location format: Location: 13.0827,80.2707
            val locRegex = Regex("LOCATION:\\s*([0-9.-]+)\\s*,\\s*([0-9.-]+)", RegexOption.IGNORE_CASE)
            val locMatch = locRegex.find(message)
            if (locMatch != null) {
                lat = locMatch.groupValues[1].toDoubleOrNull()
                lon = locMatch.groupValues[2].toDoubleOrNull()
            }
        }
        
        return ParsedSms(type, severity, speed, lat, lon)
    }
}
