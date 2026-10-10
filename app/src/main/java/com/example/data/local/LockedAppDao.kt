package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.LockedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LockedAppDao {
    @Query("SELECT * FROM locked_apps ORDER BY isFinancial DESC, appName ASC")
    fun getAllLockedApps(): Flow<List<LockedAppEntity>>

    @Query("SELECT * FROM locked_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppByPackage(packageName: String): LockedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(app: LockedAppEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(apps: List<LockedAppEntity>)

    @Update
    suspend fun update(app: LockedAppEntity)

    @Query("UPDATE locked_apps SET isLocked = :isLocked WHERE packageName = :packageName")
    suspend fun setLockedStatus(packageName: String, isLocked: Boolean)

    @Query("UPDATE locked_apps SET requireOtp = :requireOtp WHERE packageName = :packageName")
    suspend fun setOtpRequirement(packageName: String, requireOtp: Boolean)

    @Query("UPDATE locked_apps SET requireRemoteApproval = :requireRemote WHERE packageName = :packageName")
    suspend fun setRemoteApprovalRequirement(packageName: String, requireRemote: Boolean)

    @Query("SELECT COUNT(*) FROM locked_apps")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: LockedAppEntity)

    @Query("UPDATE locked_apps SET isLocked = :isLocked WHERE packageName = :packageName")
    suspend fun updateLockStatus(packageName: String, isLocked: Boolean)

    @Query("UPDATE locked_apps SET requirePin = :pin, requireOtp = :otp, requireRemoteApproval = :remote WHERE packageName = :packageName")
    suspend fun updateSecurityPolicies(packageName: String, pin: Boolean, otp: Boolean, remote: Boolean)
}
