package com.devwithguru.cricket.data.repository

import com.devwithguru.cricket.data.api.ApiService
import com.devwithguru.cricket.data.api.SyncDeliveriesRequest
import com.devwithguru.cricket.data.api.SyncDeliveryData
import com.devwithguru.cricket.data.api.SyncWicketData
import com.devwithguru.cricket.data.db.dao.PendingDeliveryDao
import com.devwithguru.cricket.data.db.entity.PendingDeliveryEntity
import com.devwithguru.cricket.data.sync.ConnectivityMonitor
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles delivery persistence (Room) and sync (API).
 *
 * Flow:
 * 1. Each scoring action → save to Room immediately (survives app crash)
 * 2. When online → batch push all pending deliveries to API
 * 3. On success → mark as synced in Room
 * 4. On connectivity restore → auto-push pending deliveries
 */
@Singleton
class DeliverySyncRepository @Inject constructor(
    private val pendingDeliveryDao: PendingDeliveryDao,
    private val apiService: ApiService,
    private val authRepository: AuthRepository,
    private val connectivityMonitor: ConnectivityMonitor
) {
    /**
     * Save a delivery to Room (called after each scoring action in LiveScorerViewModel).
     * This is synchronous with the UI — instant persistence.
     */
    suspend fun saveDelivery(delivery: PendingDeliveryEntity): Long {
        return pendingDeliveryDao.insertDelivery(delivery)
    }

    /**
     * Observe pending delivery count for a specific match (for UI sync indicator).
     */
    fun observePendingCount(matchId: String): Flow<Int> {
        return pendingDeliveryDao.observePendingCount(matchId)
    }

    /**
     * Observe total pending deliveries across all matches.
     */
    fun observeTotalPendingCount(): Flow<Int> {
        return pendingDeliveryDao.observeTotalPendingCount()
    }

    /**
     * Reconstruct scoring state from Room deliveries (for app restart recovery).
     */
    suspend fun getDeliveriesForMatch(matchId: String): List<PendingDeliveryEntity> {
        return pendingDeliveryDao.getDeliveriesForMatch(matchId)
    }

    /**
     * Get the latest delivery for a match (to check last state).
     */
    suspend fun getLatestDelivery(matchId: String): PendingDeliveryEntity? {
        return pendingDeliveryDao.getLatestDelivery(matchId)
    }

    /**
     * Push all pending deliveries for a specific match to the API.
     * Returns true if all deliveries synced successfully.
     */
    suspend fun syncMatchDeliveries(matchId: String): Boolean {
        if (!connectivityMonitor.isCurrentlyOnline()) return false

        val token = authRepository.getToken() ?: return false
        val pending = pendingDeliveryDao.getDeliveriesForMatch(matchId)
            .filter { it.syncStatus == "pending" }

        if (pending.isEmpty()) return true

        return try {
            val syncItems = pending.map { delivery ->
                SyncDeliveryData(
                    local_uuid = delivery.localUuid,
                    device_timestamp = java.text.SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                        java.util.Locale.US
                    ).format(java.util.Date(delivery.deviceTimestamp)),
                    striker_id = delivery.strikerId,
                    non_striker_id = delivery.nonStrikerId,
                    bowler_id = delivery.bowlerId,
                    runs_off_bat = delivery.runsOffBat,
                    wides = if (delivery.wides > 0) delivery.wides else null,
                    no_balls = if (delivery.noBalls > 0) delivery.noBalls else null,
                    byes = if (delivery.byes > 0) delivery.byes else null,
                    leg_byes = if (delivery.legByes > 0) delivery.legByes else null,
                    penalty_runs = if (delivery.penaltyRuns > 0) delivery.penaltyRuns else null,
                    wicket = if (delivery.wicketDismissedPlayerId != null) {
                        SyncWicketData(
                            dismissed_player_id = delivery.wicketDismissedPlayerId,
                            dismissal_type = delivery.wicketDismissalType ?: "",
                            fielder_id = delivery.wicketFielderId,
                            runs_completed = delivery.wicketRunsCompleted
                        )
                    } else null
                )
            }

            // Mark as syncing
            pending.forEach { delivery ->
                pendingDeliveryDao.updateSyncStatus(delivery.id, "syncing")
            }

            val request = SyncDeliveriesRequest(deliveries = syncItems)
            val response = apiService.syncDeliveries(token, matchId, request)

            if (response.isSuccessful) {
                // Mark all as synced
                pendingDeliveryDao.markSynced(pending.map { it.id })
                // Cleanup old synced deliveries
                pendingDeliveryDao.deleteSynced()
                pendingDeliveryDao.deleteOldFailed()
                true
            } else {
                // Mark as failed
                val errorMsg = response.errorBody()?.string() ?: "Sync failed"
                pending.forEach { delivery ->
                    pendingDeliveryDao.updateSyncStatus(delivery.id, "failed", errorMsg)
                    pendingDeliveryDao.incrementRetry(delivery.id)
                }
                false
            }
        } catch (e: Exception) {
            // Mark as failed
            pending.forEach { delivery ->
                pendingDeliveryDao.updateSyncStatus(delivery.id, "failed", e.message)
                pendingDeliveryDao.incrementRetry(delivery.id)
            }
            false
        }
    }

    /**
     * Push pending deliveries for ALL matches (called during full sync).
     */
    suspend fun syncAllPendingDeliveries(): Pair<Int, Int> {
        if (!connectivityMonitor.isCurrentlyOnline()) return Pair(0, 0)

        val allPending = pendingDeliveryDao.getAllPending()
        val matchIds = allPending.map { it.matchId }.distinct()

        var successCount = 0
        var failCount = 0

        for (matchId in matchIds) {
            val success = syncMatchDeliveries(matchId)
            if (success) successCount++ else failCount++
        }

        return Pair(successCount, failCount)
    }
}
