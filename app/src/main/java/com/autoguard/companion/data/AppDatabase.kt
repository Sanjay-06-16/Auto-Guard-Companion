package com.autoguard.companion.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.autoguard.companion.data.dao.AlertDao
import com.autoguard.companion.data.dao.ContactDao
import com.autoguard.companion.data.dao.ProfileDao
import com.autoguard.companion.data.entity.AlertEntity
import com.autoguard.companion.data.entity.ContactEntity
import com.autoguard.companion.data.entity.ProfileEntity

@Database(entities = [AlertEntity::class, ProfileEntity::class, ContactEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun alertDao(): AlertDao
    abstract fun profileDao(): ProfileDao
    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autoguard_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
