package com.autoguard.companion.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "ACCIDENT", "LOCATION_REQUEST", etc.
    val severity: String, // "MINOR", "MODERATE", "SEVERE", "UNKNOWN"
    val speed: Int?,
    val latitude: Double?,
    val longitude: Double?,
    val timestamp: Long,
    val rawMessage: String,
    val isViewed: Boolean = false,
    val locationSource: String? = null, // "ESP32", "PHONE", "NONE"
    val smsStatus: String? = null // "SENT", "FAILED", "NOT_SENT"
)
