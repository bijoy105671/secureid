package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AuditLogEntity
import com.example.data.model.CloudDriveItemEntity
import com.example.data.model.LockedAppEntity
import com.example.data.model.RemoteRequestEntity
import com.example.data.model.SensorAccessLogEntity
import com.example.data.model.VaultFileEntity

@Database(
    entities = [
        VaultFileEntity::class,
        LockedAppEntity::class,
        RemoteRequestEntity::class,
        AuditLogEntity::class,
        SensorAccessLogEntity::class,
        CloudDriveItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vaultFileDao(): VaultFileDao
    abstract fun lockedAppDao(): LockedAppDao
    abstract fun remoteRequestDao(): RemoteRequestDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun sensorAccessDao(): SensorAccessDao
    abstract fun cloudDriveDao(): CloudDriveDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "secureshield_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
