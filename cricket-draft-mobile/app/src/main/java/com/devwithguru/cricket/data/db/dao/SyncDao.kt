package com.devwithguru.cricket.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.devwithguru.cricket.data.db.entity.PendingChangeEntity
import com.devwithguru.cricket.data.db.entity.SyncStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncStatusDao {
    @Query("SELECT * FROM sync_status WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun getSyncStatus(entityType: String, entityId: String): SyncStatusEntity?

    @Query("SELECT * FROM sync_status WHERE isDirty = 1")
    suspend fun getDirtyRecords(): List<SyncStatusEntity>

    @Query("SELECT * FROM sync_status WHERE isDirty = 1")
    fun observeDirtyRecords(): Flow<List<SyncStatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncStatus(status: SyncStatusEntity)

    @Query("UPDATE sync_status SET isDirty = :isDirty WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun setDirty(entityType: String, entityId: String, isDirty: Boolean)

    @Query("DELETE FROM sync_status WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteSyncStatus(entityType: String, entityId: String)
}

@Dao
interface PendingChangeDao {
    @Query("SELECT * FROM pending_changes WHERE status = 'pending' ORDER BY createdAt ASC")
    suspend fun getPendingChanges(): List<PendingChangeEntity>

    @Query("SELECT * FROM pending_changes WHERE status = 'pending' ORDER BY createdAt ASC")
    fun observePendingChanges(): Flow<List<PendingChangeEntity>>

    @Query("SELECT COUNT(*) FROM pending_changes WHERE status = 'pending'")
    fun observePendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChange(change: PendingChangeEntity): Long

    @Update
    suspend fun updateChange(change: PendingChangeEntity)

    @Query("UPDATE pending_changes SET status = :status, lastError = :error WHERE id = :id")
    suspend fun updateChangeStatus(id: Long, status: String, error: String? = null)

    @Query("UPDATE pending_changes SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetryCount(id: Long)

    @Query("DELETE FROM pending_changes WHERE id = :id")
    suspend fun deleteChange(id: Long)

    @Query("DELETE FROM pending_changes WHERE status = 'completed'")
    suspend fun deleteCompletedChanges()

    @Query("DELETE FROM pending_changes WHERE retryCount >= :maxRetries AND status = 'failed'")
    suspend fun deleteFailedChanges(maxRetries: Int = 5)

    /**
     * Requeue rows that would never be retried otherwise. The push loop only
     * SELECTs status='pending', but it first marks rows 'syncing' (then
     * 'failed' on error). A crash mid-push leaves rows stuck in 'syncing'
     * forever, and a failed row stays dead until the retryCount purge — both
     * silently drop the change (e.g. a lineup player that never reaches the
     * backend). Call at the start of every push cycle.
     */
    @Query("UPDATE pending_changes SET status = 'pending' WHERE status IN ('syncing', 'failed')")
    suspend fun requeueStuckChanges()
}
