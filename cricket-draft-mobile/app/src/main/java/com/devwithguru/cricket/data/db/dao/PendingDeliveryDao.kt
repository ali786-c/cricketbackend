package com.devwithguru.cricket.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devwithguru.cricket.data.db.entity.PendingDeliveryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingDeliveryDao {

    /** Insert a single delivery (after scoring action). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(delivery: PendingDeliveryEntity): Long

    /** Get all pending deliveries for a specific match (ordered by time). */
    @Query("SELECT * FROM pending_deliveries WHERE matchId = :matchId ORDER BY deviceTimestamp ASC")
    suspend fun getDeliveriesForMatch(matchId: String): List<PendingDeliveryEntity>

    /** Observe pending deliveries count for a match (for UI sync indicator). */
    @Query("SELECT COUNT(*) FROM pending_deliveries WHERE matchId = :matchId AND syncStatus = 'pending'")
    fun observePendingCount(matchId: String): Flow<Int>

    /** Get all unsynced deliveries across all matches (for full sync). */
    @Query("SELECT * FROM pending_deliveries WHERE syncStatus = 'pending' ORDER BY deviceTimestamp ASC")
    suspend fun getAllPending(): List<PendingDeliveryEntity>

    /** Get all unsynced deliveries across all matches (Flow for UI). */
    @Query("SELECT COUNT(*) FROM pending_deliveries WHERE syncStatus = 'pending'")
    fun observeTotalPendingCount(): Flow<Int>

    /** Update sync status after API push. */
    @Query("UPDATE pending_deliveries SET syncStatus = :status, lastError = :error WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, error: String? = null)

    /** Bulk update sync status after successful batch sync. */
    @Query("UPDATE pending_deliveries SET syncStatus = 'synced' WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    /** Increment retry count on failure. */
    @Query("UPDATE pending_deliveries SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: Long)

    /** Delete deliveries that have been synced (cleanup). */
    @Query("DELETE FROM pending_deliveries WHERE syncStatus = 'synced'")
    suspend fun deleteSynced()

    /** Delete old failed deliveries that exceeded max retries. */
    @Query("DELETE FROM pending_deliveries WHERE retryCount >= :maxRetries AND syncStatus = 'failed'")
    suspend fun deleteOldFailed(maxRetries: Int = 5)

    /** Get the latest delivery for a match (to reconstruct state). */
    @Query("SELECT * FROM pending_deliveries WHERE matchId = :matchId ORDER BY deviceTimestamp DESC LIMIT 1")
    suspend fun getLatestDelivery(matchId: String): PendingDeliveryEntity?

    /** Get all deliveries for a match to reconstruct scoring state on app restart. */
    @Query("SELECT * FROM pending_deliveries WHERE matchId = :matchId ORDER BY deviceTimestamp ASC")
    fun observeDeliveriesForMatch(matchId: String): Flow<List<PendingDeliveryEntity>>

    /** Count deliveries for a match. */
    @Query("SELECT COUNT(*) FROM pending_deliveries WHERE matchId = :matchId")
    suspend fun getDeliveryCount(matchId: String): Int
}
