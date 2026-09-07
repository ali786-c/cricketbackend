package com.devwithguru.cricket.data.sync

import com.devwithguru.cricket.data.api.ApiService
import com.devwithguru.cricket.data.api.UpdateFixtureStatusRequest
import com.devwithguru.cricket.data.db.dao.AdminFixtureDao
import com.devwithguru.cricket.data.db.dao.PendingChangeDao
import com.devwithguru.cricket.data.db.dao.SyncStatusDao
import com.devwithguru.cricket.data.db.dao.TeamDao
import com.devwithguru.cricket.data.db.dao.TournamentDao
import com.devwithguru.cricket.data.db.entity.PendingChangeEntity
import com.devwithguru.cricket.data.db.entity.SyncStatusEntity
import com.devwithguru.cricket.data.mapper.toEntity
import com.devwithguru.cricket.data.repository.AuthRepository
import com.devwithguru.cricket.data.repository.DeliverySyncRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates bidirectional sync between local Room database and remote API.
 *
 * Sync Strategy (dedup-safe):
 * 1. READS: Always read from local Room first (fast, offline-capable)
 * 2. WRITES: Write to local Room immediately, queue for API sync
 * 3. PUSH: When online, push pending changes to API, then MAP local ID → server ID
 * 4. PULL: When online, pull from API, UPSERT by serverId (no duplicates)
 * 5. CONFLICT: Server timestamp wins (last-write-wins with server priority)
 * 6. AUTO-SYNC: When connectivity restored, push then pull
 * 7. DELIVERY SYNC: Batch sync deliveries with dedup by local_uuid
 *
 * Dedup Rules:
 * - Tournaments: matched by serverId (API returns integer IDs)
 * - Teams: matched by serverId within a tournament
 * - Fixtures: matched by serverId OR by composite key (tournament + home + away team)
 * - Deliveries: matched by local_uuid (server deduplicates on insert)
 */
@Singleton
class SyncManager @Inject constructor(
    private val connectivityMonitor: ConnectivityMonitor,
    private val syncStatusDao: SyncStatusDao,
    private val pendingChangeDao: PendingChangeDao,
    private val tournamentDao: TournamentDao,
    private val teamDao: TeamDao,
    private val adminFixtureDao: AdminFixtureDao,
    private val apiService: ApiService,
    private val authRepository: AuthRepository,
    private val gson: Gson,
    private val deliverySyncRepository: DeliverySyncRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val autoSyncListener: () -> Unit = {
        scope.launch { onConnectivityRestored() }
    }

    init {
        connectivityMonitor.onConnectivityRestored(autoSyncListener)
    }

    /** Check if device is currently online. */
    fun isOnline(): Boolean = connectivityMonitor.isCurrentlyOnline()

    // ─── Entity Tracking ───────────────────────────────────────

    suspend fun markDirty(entityType: String, entityId: String) {
        syncStatusDao.upsertSyncStatus(
            SyncStatusEntity(entityType = entityType, entityId = entityId, isDirty = true, lastSyncedAt = System.currentTimeMillis())
        )
        updatePendingCount()
    }

    suspend fun markSynced(entityType: String, entityId: String, serverVersion: Int = 0) {
        syncStatusDao.upsertSyncStatus(
            SyncStatusEntity(entityType = entityType, entityId = entityId, isDirty = false, lastSyncedAt = System.currentTimeMillis(), serverVersion = serverVersion)
        )
    }

    suspend fun queueChange(entityType: String, entityId: String, action: String, payload: Any) {
        val change = PendingChangeEntity(entityType = entityType, entityId = entityId, action = action, payload = gson.toJson(payload))
        pendingChangeDao.insertChange(change)
        markDirty(entityType, entityId)
        updatePendingCount()
    }

    // ─── PUSH (Mobile → Web) ──────────────────────────────────

    suspend fun pushPendingChanges(): SyncResult {
        if (!connectivityMonitor.isCurrentlyOnline()) return SyncResult(false, "Offline")

        val token = authRepository.getRawToken() ?: return SyncResult(false, "Not authenticated")
        _isSyncing.value = true
        _syncStatus.value = SyncStatus.PUSHING
        var successCount = 0
        var failCount = 0

        try {
            val pendingChanges = pendingChangeDao.getPendingChanges()
            for (change in pendingChanges) {
                try {
                    pendingChangeDao.updateChangeStatus(change.id, "syncing")
                    val success = pushSingleChange(change, token)
                    if (success) {
                        pendingChangeDao.updateChangeStatus(change.id, "completed")
                        markSynced(change.entityType, change.entityId)
                        successCount++
                    } else {
                        pendingChangeDao.updateChangeStatus(change.id, "failed", "API error")
                        pendingChangeDao.incrementRetryCount(change.id)
                        failCount++
                    }
                } catch (e: Exception) {
                    pendingChangeDao.updateChangeStatus(change.id, "failed", e.message)
                    pendingChangeDao.incrementRetryCount(change.id)
                    failCount++
                }
            }

            // Push pending deliveries
            val (deliverySuccess, deliveryFail) = deliverySyncRepository.syncAllPendingDeliveries()
            successCount += deliverySuccess
            failCount += deliveryFail

            pendingChangeDao.deleteCompletedChanges()
            pendingChangeDao.deleteFailedChanges(maxRetries = 5)
            _lastSyncTime.value = System.currentTimeMillis()
            updatePendingCount()
            _syncStatus.value = if (failCount == 0) SyncStatus.IDLE else SyncStatus.PARTIAL
            _syncMessage.value = "Pushed $successCount, failed $failCount"
            return SyncResult(failCount == 0, "Pushed $successCount, failed $failCount")
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun pushSingleChange(change: PendingChangeEntity, token: String): Boolean {
        val payload = gson.fromJson(change.payload, Map::class.java) as? Map<*, *> ?: return true
        val authHeader = "Bearer $token"

        return try {
            when (change.entityType) {
                "admin_team" -> pushTeamChange(change, payload, authHeader)
                "admin_player" -> pushPlayerChange(payload, authHeader)
                "admin_fixture" -> pushFixtureChange(change, payload, authHeader)
                "fixture" -> pushFixtureChange(change, payload, authHeader)  // Also handle from MainViewModel.startMatch()
                "admin_draft_setup" -> true
                "tournament_status" -> pushTournamentStatusChange(payload, authHeader)
                "captain" -> pushCaptainChange(payload, authHeader)
                "delivery" -> true // handled by DeliverySyncRepository
                else -> true
            }
        } catch (e: Exception) { false }
    }

    /**
     * Push team create → server returns server ID → update local entity with server ID.
     * This ensures next pull won't create a duplicate.
     */
    private suspend fun pushTeamChange(change: PendingChangeEntity, payload: Map<*, *>, authHeader: String): Boolean {
        val tournamentId = payload["tournamentId"] as? String ?: return false
        return when (change.action) {
            "create" -> {
                val name = payload["name"] as? String ?: return false
                val shortName = payload["shortName"] as? String
                val body = com.devwithguru.cricket.data.api.CreateTeamRequest(name, shortName)
                val response = apiService.createTeam(authHeader, tournamentId, body)
                if (response.isSuccessful) {
                    val serverId = response.body()?.data?.id
                    if (serverId != null) {
                        // MAP local ID → server ID so next pull won't duplicate
                        teamDao.updateServerId(change.entityId, serverId)
                    }
                    true
                } else false
            }
            "delete" -> {
                val serverId = (payload["serverId"] as? Number)?.toInt() ?: return true
                val response = apiService.deleteTeam(authHeader, tournamentId, serverId.toString())
                response.isSuccessful
            }
            else -> true
        }
    }

    private suspend fun pushPlayerChange(payload: Map<*, *>, authHeader: String): Boolean {
        val tournamentId = payload["tournamentId"] as? String ?: return false
        return when (payload["action"] as? String) {
            "approve" -> {
                val playerId = payload["playerId"] as? String ?: return false
                apiService.approvePlayer(authHeader, tournamentId, playerId).isSuccessful
            }
            "reject" -> {
                val playerId = payload["playerId"] as? String ?: return false
                apiService.rejectPlayer(authHeader, tournamentId, playerId).isSuccessful
            }
            else -> true
        }
    }

    /**
     * Push fixture create → server returns server ID → update local entity.
     */
    private suspend fun pushFixtureChange(change: PendingChangeEntity, payload: Map<*, *>, authHeader: String): Boolean {
        val tournamentId = payload["tournamentId"] as? String ?: return false
        return when (change.action) {
            "create" -> {
                val body = com.devwithguru.cricket.data.api.CreateFixtureRequest(
                    round_number = (payload["roundNumber"] as? Number)?.toInt() ?: 1,
                    round_name = payload["roundName"] as? String ?: "",
                    match_number = (payload["matchNumber"] as? Number)?.toInt() ?: 1,
                    home_team_id = (payload["homeTeamId"] as? String)?.toIntOrNull() ?: 0,
                    away_team_id = (payload["awayTeamId"] as? String)?.toIntOrNull() ?: 0,
                    scheduled_at = (payload["scheduledAt"] as? String)
                        ?: run {
                            val date = payload["scheduledDate"] as? String ?: ""
                            val time = payload["scheduledTime"] as? String ?: "00:00"
                            if (date.isNotBlank()) "${date}T${time}:00.000000Z" else ""
                        },
                    venue = payload["venue"] as? String,
                    city = payload["city"] as? String
                )
                val response = apiService.createFixture(authHeader, tournamentId, body)
                if (response.isSuccessful) {
                    val serverId = response.body()?.data?.id
                    if (serverId != null) {
                        // MAP local ID → server ID
                        adminFixtureDao.updateServerIdAndSync(change.entityId, serverId, "synced")
                    }
                    true
                } else false
            }
            "update" -> {
                // Handle status updates (like live/completed) via API
                val status = payload["status"] as? String
                val tournamentId = payload["tournamentId"] as? String
                if (status != null && tournamentId != null) {
                    try {
                        apiService.updateFixtureStatus(authHeader, tournamentId, change.entityId, UpdateFixtureStatusRequest(status)).isSuccessful
                    } catch (_: Exception) { false }
                } else {
                    adminFixtureDao.updateSyncStatus(change.entityId, "synced")
                    true
                }
            }
            "delete" -> {
                val serverFixtureId = (payload["serverId"] as? String)?.toIntOrNull()
                if (serverFixtureId != null) {
                    apiService.deleteFixture(authHeader, tournamentId, serverFixtureId.toString()).isSuccessful
                } else true
            }
            else -> true
        }
    }

    private suspend fun pushTournamentStatusChange(payload: Map<*, *>, authHeader: String): Boolean {
        val tournamentId = payload["tournamentId"] as? String ?: return false
        val status = payload["status"] as? String ?: return false
        val body = com.devwithguru.cricket.data.api.UpdateStatusRequest(status)
        return apiService.updateTournamentStatus(authHeader, tournamentId, body).isSuccessful
    }

    private suspend fun pushCaptainChange(payload: Map<*, *>, authHeader: String): Boolean {
        val tournamentId = payload["tournamentId"] as? String ?: return false
        val teamId = payload["teamId"] as? String ?: return false
        val userId = (payload["userId"] as? Number)?.toInt() ?: return false
        val body = com.devwithguru.cricket.data.api.AssignCaptainRequest(userId)
        return apiService.assignCaptain(authHeader, tournamentId, teamId, body).isSuccessful
    }

    // ─── PULL (Web → Mobile) with DEDUP ────────────────────────

    /**
     * Pull latest data from API with deduplication.
     *
     * Dedup strategy:
     * - Tournaments: matched by serverId. If local exists with same serverId → UPDATE (server wins).
     *   If no match → INSERT. Local-only tournaments (not on server) are preserved.
     * - Teams: matched by serverId within tournament. Same logic.
     * - Fixtures: matched by serverId OR composite key (tournament+home+away).
     */
    suspend fun pullLatestData(): SyncResult {
        if (!connectivityMonitor.isCurrentlyOnline()) return SyncResult(false, "Offline")

        _isSyncing.value = true
        _syncStatus.value = SyncStatus.PULLING
        var tournamentCount = 0
        var teamCount = 0
        var fixtureCount = 0

        try {
            // 1. Pull tournaments with dedup
            try {
                val tournamentsResult = apiService.getTournaments()
                if (tournamentsResult.isSuccessful) {
                    val tournaments = tournamentsResult.body()?.data ?: emptyList()
                    for (tournament in tournaments) {
                        val serverEntity = tournament.toEntity() // serverId is set in mapper
                        val existingByServerId = tournamentDao.findByServerId(tournament.id)

                        if (existingByServerId != null) {
                            // Server wins: update local entity with server data
                            // But preserve local-only fields (like localId for offline scoring)
                            val updated = serverEntity.copy(
                                id = existingByServerId.id, // Keep local ID
                                serverId = tournament.id,
                                updatedAt = System.currentTimeMillis()
                            )
                            tournamentDao.insertTournament(updated) // REPLACE on conflict
                        } else {
                            // New from server — insert with server ID
                            tournamentDao.insertTournament(serverEntity)
                        }
                        tournamentCount++

                        // 2. Pull teams for this tournament with dedup
                        pullTeamsForTournament(tournament.id.toString(), authHeader = "Bearer ${authRepository.getRawToken() ?: ""}")

                        // 3. Pull fixtures for this tournament with dedup
                        pullFixturesForTournament(tournament.id.toString(), authHeader = "Bearer ${authRepository.getRawToken() ?: ""}")
                    }
                }
            } catch (_: Exception) { }

            _lastSyncTime.value = System.currentTimeMillis()
            _syncStatus.value = SyncStatus.IDLE
            _syncMessage.value = "Pulled $tournamentCount tournaments, $teamCount teams, $fixtureCount fixtures"

            return SyncResult(true, "Synced: $tournamentCount tournaments, $teamCount teams, $fixtureCount fixtures")
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.ERROR
            _syncMessage.value = "Pull failed: ${e.message}"
            return SyncResult(false, e.message ?: "Sync failed")
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * Pull teams for a tournament with dedup by serverId.
     */
    private suspend fun pullTeamsForTournament(tournamentId: String, authHeader: String) {
        try {
            val teamsResult = apiService.getAdminTeams(authHeader, tournamentId)
            if (teamsResult.isSuccessful) {
                val teams = teamsResult.body()?.data ?: emptyList()
                for (teamData in teams) {
                    val serverId = teamData.id ?: continue
                    val existingByServerId = teamDao.findByServerId(serverId)

                    if (existingByServerId != null) {
                        // Server wins: update existing local entity
                        val updated = existingByServerId.copy(
                            name = teamData.name ?: existingByServerId.name,
                            shortName = teamData.short_name ?: existingByServerId.shortName,
                            updatedAt = System.currentTimeMillis()
                        )
                        teamDao.insertTeam(updated)
                    } else {
                        // New from server — insert
                        val newEntity = com.devwithguru.cricket.data.db.entity.TeamEntity(
                            id = serverId.toString(),
                            serverId = serverId,
                            name = teamData.name ?: "",
                            shortName = teamData.short_name ?: "",
                            tournamentId = tournamentId,
                            updatedAt = System.currentTimeMillis()
                        )
                        teamDao.insertTeam(newEntity)
                    }
                }
            }
        } catch (_: Exception) { }
    }

    /**
     * Pull fixtures for a tournament with dedup by serverId or composite key.
     */
    private suspend fun pullFixturesForTournament(tournamentId: String, authHeader: String) {
        try {
            val fixturesResult = apiService.getAdminFixtures(authHeader, tournamentId)
            if (fixturesResult.isSuccessful) {
                val fixtures = fixturesResult.body()?.data ?: emptyList()
                for (fixtureData in fixtures) {
                    val serverId = fixtureData.id
                    val homeTeamId = fixtureData.home_team?.id?.toString() ?: ""
                    val awayTeamId = fixtureData.away_team?.id?.toString() ?: ""

                    // Dedup 1: by serverId
                    val existingByServerId = adminFixtureDao.findByServerId(serverId)

                    if (existingByServerId != null) {
                        // Server wins: update existing
                        val updated = existingByServerId.copy(
                            status = fixtureData.status ?: existingByServerId.status,
                            venue = fixtureData.venue ?: existingByServerId.venue,
                            city = fixtureData.city ?: existingByServerId.city,
                            tossWinner = fixtureData.toss_winner ?: existingByServerId.tossWinner,
                            tossDecision = fixtureData.toss_decision ?: existingByServerId.tossDecision,
                            updatedAt = System.currentTimeMillis()
                        )
                        adminFixtureDao.insert(updated)
                    } else {
                        // Dedup 2: by composite key (tournament + home + away)
                        val existingByTeams = adminFixtureDao.findByTeams(tournamentId, homeTeamId, awayTeamId)
                        if (existingByTeams != null) {
                            // Local fixture exists — update with server ID and latest data
                            val updated = existingByTeams.copy(
                                serverId = serverId,
                                status = fixtureData.status ?: existingByTeams.status,
                                venue = fixtureData.venue ?: existingByTeams.venue,
                                tossWinner = fixtureData.toss_winner ?: existingByTeams.tossWinner,
                                tossDecision = fixtureData.toss_decision ?: existingByTeams.tossDecision,
                                syncStatus = "synced",
                                updatedAt = System.currentTimeMillis()
                            )
                            adminFixtureDao.insert(updated)
                        } else {
                            // Truly new fixture from server — insert
                            val newEntity = com.devwithguru.cricket.data.db.entity.AdminFixtureEntity(
                                id = "srv_${tournamentId}_${serverId}",
                                serverId = serverId,
                                tournamentId = tournamentId,
                                roundNumber = fixtureData.round_number ?: 1,
                                roundName = fixtureData.round_name ?: "",
                                matchNumber = fixtureData.match_number ?: 1,
                                homeTeamId = homeTeamId,
                                homeTeamName = fixtureData.home_team?.name ?: "",
                                awayTeamId = awayTeamId,
                                awayTeamName = fixtureData.away_team?.name ?: "",
                                scheduledDate = fixtureData.scheduled_at?.split("T")?.getOrNull(0),
                                scheduledTime = fixtureData.scheduled_at?.split("T")?.getOrNull(1)?.split(".")?.getOrNull(0),
                                venue = fixtureData.venue,
                                city = fixtureData.city,
                                status = fixtureData.status ?: "scheduled",
                                tossWinner = fixtureData.toss_winner,
                                tossDecision = fixtureData.toss_decision,
                                syncStatus = "synced",
                                updatedAt = System.currentTimeMillis()
                            )
                            adminFixtureDao.insert(newEntity)
                        }
                    }
                }
            }
        } catch (_: Exception) { }
    }

    // ─── Full Sync ──────────────────────────────────────────────

    suspend fun fullSync(): SyncResult {
        _syncStatus.value = SyncStatus.SYNCING
        val pushResult = pushPendingChanges()
        val pullResult = pullLatestData()

        return when {
            pushResult.success && pullResult.success -> {
                _syncStatus.value = SyncStatus.IDLE
                SyncResult(true, "Full sync complete")
            }
            pushResult.success -> pullResult
            else -> pushResult
        }.also { _syncMessage.value = it.message }
    }

    /**
     * Pre-sync before live scoring: push all local changes first, then pull server data.
     * This ensures mobile-created offline data (fixtures, teams, toss) reaches the server
     * before scoring begins, and any web-created data is pulled down.
     *
     * Call this before: starting a match, loading match center, going live.
     */
    suspend fun preSyncBeforeLiveScoring(): SyncResult {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return SyncResult(true, "Offline — using local data")
        }

        _syncMessage.value = "Syncing before going live..."

        // Step 1: Push ALL local changes to server first
        val pushResult = pushPendingChanges()

        // Step 2: Pull latest from server (get web-created data)
        val pullResult = pullLatestData()

        // Step 3: Push deliveries too
        deliverySyncRepository.syncAllPendingDeliveries()

        return when {
            pushResult.success && pullResult.success -> {
                _syncMessage.value = "Pre-sync complete ✓"
                SyncResult(true, "Data synced — ready to score")
            }
            pushResult.success -> pullResult
            else -> pushResult
        }
    }

    suspend fun onConnectivityRestored() {
        if (connectivityMonitor.isCurrentlyOnline()) {
            _syncMessage.value = "Network restored — syncing..."
            pushPendingChanges()
            deliverySyncRepository.syncAllPendingDeliveries()
            _syncMessage.value = "Auto-sync complete"
        }
    }

    private suspend fun updatePendingCount() {
        _pendingCount.value = pendingChangeDao.getPendingChanges().size
    }
}

data class SyncResult(val success: Boolean, val message: String)

enum class SyncStatus { IDLE, SYNCING, PUSHING, PULLING, PARTIAL, ERROR }
