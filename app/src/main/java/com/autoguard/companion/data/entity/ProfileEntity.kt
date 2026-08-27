package com.autoguard.companion.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1, // Only one profile row
    val name: String,
    val bloodGroup: String,
    val bikeNumber: String,
    val bikeGsmNumber: String,
    val riderPhoneNumber: String = "",
    val emergencyContact1: String = "",
    val emergencyContact2: String = "",
    val medicalNotes: String
)
