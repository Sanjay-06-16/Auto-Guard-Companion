package com.autoguard.companion.bluetooth

data class AccidentEvent(
    val severity: String,
    val speed: Int,
    val esp32GpsAvailable: Boolean,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

object AccidentParser {
    /**
     * Parses a packet in the format:
     * AUTOGUARD|ACCIDENT|SEVERE|SPEED:82|GPS:YES|LAT:13.082680|LON:80.270718
     */
    fun parse(packet: String): AccidentEvent? {
        try {
            val parts = packet.trim().split("|")
            // Required Format: AUTOGUARD|ACCIDENT|SEVERITY|SPEED:X|GPS:YES/NO|LAT:X|LON:X
            if (parts.size < 7 || parts[0] != "AUTOGUARD" || parts[1] != "ACCIDENT") return null

            val severity = parts[2]
            val speed = parts[3].substringAfter("SPEED:").toIntOrNull() ?: 0
            val gpsAvailable = parts[4].substringAfter("GPS:") == "YES"
            val lat = parts[5].substringAfter("LAT:").toDoubleOrNull() ?: 0.0
            val lon = parts[6].substringAfter("LON:").toDoubleOrNull() ?: 0.0

            return AccidentEvent(
                severity = severity,
                speed = speed,
                esp32GpsAvailable = gpsAvailable,
                latitude = lat,
                longitude = lon
            )
        } catch (e: Exception) {
            return null
        }
    }
}
