package com.autoguard.companion.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.autoguard.companion.data.entity.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE type = :type ORDER BY timestamp DESC")
    fun getAlertsByType(type: String): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE id = :id")
    suspend fun getAlertById(id: Int): AlertEntity?

    @Query("SELECT * FROM alerts WHERE type = 'LOCATION' ORDER BY timestamp DESC LIMIT 1")
    fun getLatestLocation(): Flow<AlertEntity?>

    @Insert
    suspend fun insertAlert(alert: AlertEntity)

    @Update
    suspend fun updateAlert(alert: AlertEntity)
    
    @Query("DELETE FROM alerts")
    suspend fun deleteAll()
}
