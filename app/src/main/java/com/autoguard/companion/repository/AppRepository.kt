package com.autoguard.companion.repository

import com.autoguard.companion.data.dao.AlertDao
import com.autoguard.companion.data.dao.ContactDao
import com.autoguard.companion.data.dao.ProfileDao
import com.autoguard.companion.data.entity.AlertEntity
import com.autoguard.companion.data.entity.ContactEntity
import com.autoguard.companion.data.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val alertDao: AlertDao,
    private val profileDao: ProfileDao,
    private val contactDao: ContactDao
) {
    fun getAllAlerts(): Flow<List<AlertEntity>> = alertDao.getAllAlerts()
    fun getAlertsByType(type: String): Flow<List<AlertEntity>> = alertDao.getAlertsByType(type)
    suspend fun getAlertById(id: Int): AlertEntity? = alertDao.getAlertById(id)
    fun getLatestLocation(): Flow<AlertEntity?> = alertDao.getLatestLocation()
    
    suspend fun insertAlert(alert: AlertEntity) = alertDao.insertAlert(alert)
    suspend fun updateAlert(alert: AlertEntity) = alertDao.updateAlert(alert)
    
    fun getProfile(): Flow<ProfileEntity?> = profileDao.getProfile()
    suspend fun insertOrUpdateProfile(profile: ProfileEntity) = profileDao.insertOrUpdateProfile(profile)
    
    fun getAllContacts(): Flow<List<ContactEntity>> = contactDao.getAllContacts()
    suspend fun insertContact(contact: ContactEntity) = contactDao.insertContact(contact)
    suspend fun deleteContact(contact: ContactEntity) = contactDao.deleteContact(contact)
    
    suspend fun clearAllAlerts() = alertDao.deleteAll()
}
