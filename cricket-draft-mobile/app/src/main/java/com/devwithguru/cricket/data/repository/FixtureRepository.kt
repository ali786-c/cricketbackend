package com.devwithguru.cricket.data.repository

import com.devwithguru.cricket.data.db.dao.AdminFixtureDao
import com.devwithguru.cricket.data.db.entity.AdminFixtureEntity
import com.devwithguru.cricket.data.mapper.toDomain
import com.devwithguru.cricket.data.mapper.toEntity
import com.devwithguru.cricket.data.mapper.toScheduledDomain
import com.devwithguru.cricket.data.sync.SyncManager
import com.devwithguru.cricket.domain.model.Fixture
import com.devwithguru.cricket.domain.model.ScheduledFixture
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first repository for fixture management — PRD §21-§22
 *
 * Strategy:
 * 1. WRITE to Room immediately (instant UI feedback)
 * 2. Queue change for API sync
 * 3. When online, SyncManager pushes pending changes
 */
@Singleton
class FixtureRepository @Inject constructor(
    private val adminFixtureDao: AdminFixtureDao,
    private val fixtureDao: com.devwithguru.cricket.data.db.dao.FixtureDao,
    private val syncManager: SyncManager,
    private val tournamentDao: com.devwithguru.cricket.data.db.dao.TournamentDao
) {
    /**
     * Get all fixtures for a tournament (Flow)
     */
    fun getFixtures(tournamentId: String): Flow<List<Fixture>> =
        adminFixtureDao.getByTournament(tournamentId).map { entities ->
            entities.map { it.toDomain() }
        }

    fun getFixturesByTeam(teamId: String): Flow<List<Fixture>> =
        adminFixtureDao.getByTeam(teamId).map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * Get fixtures for a specific stage (Flow)
     */
    fun getFixturesByStage(stageId: String): Flow<List<Fixture>> =
        adminFixtureDao.getByStage(stageId).map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * Get fixtures by status (Flow)
     */
    fun getFixturesByStatus(tournamentId: String, status: String): Flow<List<Fixture>> =
        adminFixtureDao.getByStatus(tournamentId, status).map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * Get a single fixture by ID
     */
    suspend fun getFixtureById(fixtureId: String): Fixture? =
        adminFixtureDao.findById(fixtureId)?.toDomain()

    /**
     * Schedule a new match (create fixture) — PRD §21
     */
    suspend fun scheduleMatch(
        tournamentId: String,
        stageId: String? = null,
        stageName: String? = null,
        homeTeamId: String,
        homeTeamName: String,
        awayTeamId: String,
        awayTeamName: String,
        scheduledDate: String,
        scheduledTime: String,
        venue: String? = null,
        city: String? = null,
        matchType: String = "normal",
        umpire1: String? = null,
        umpire2: String? = null,
        roundNumber: Int = 1,
        roundName: String = ""
    ): Fixture {
        val id = "fx_${tournamentId}_${System.currentTimeMillis()}"
        val entity = AdminFixtureEntity(
            id = id,
            tournamentId = tournamentId,
            stageId = stageId,
            stageName = stageName,
            roundNumber = roundNumber,
            roundName = roundName,
            homeTeamId = homeTeamId,
            homeTeamName = homeTeamName,
            awayTeamId = awayTeamId,
            awayTeamName = awayTeamName,
            scheduledDate = scheduledDate,
            scheduledTime = scheduledTime,
            scheduledAt = "$scheduledDate $scheduledTime",
            venue = venue,
            city = city,
            matchType = matchType,
            umpire1 = umpire1,
            umpire2 = umpire2,
            status = "scheduled",
            syncStatus = "pending"
        )
        adminFixtureDao.insert(entity)

        syncManager.queueChange("fixture", id, "create", mapOf(
            "tournamentId" to tournamentId,
            "stageId" to (stageId ?: ""),
            "homeTeamId" to homeTeamId,
            "awayTeamId" to awayTeamId,
            "scheduledDate" to scheduledDate,
            "scheduledTime" to scheduledTime,
            "venue" to (venue ?: "")
        ))

        return entity.toDomain()
    }

    /**
     * Update fixture schedule (date, time, venue) — PRD §22
     */
    suspend fun updateSchedule(
        fixtureId: String,
        date: String? = null,
        time: String? = null,
        venue: String? = null,
        city: String? = null
    ) {
        adminFixtureDao.updateSchedule(fixtureId, date, time, venue, city)
        val fixture = adminFixtureDao.findById(fixtureId) ?: return
        if (fixture.serverId != null) {
            syncManager.queueChange("fixture", fixtureId, "update", mapOf(
                "tournamentId" to fixture.tournamentId,
                "scheduledDate" to (date ?: fixture.scheduledDate ?: ""),
                "scheduledTime" to (time ?: fixture.scheduledTime ?: ""),
                "venue" to (venue ?: fixture.venue ?: "")
            ))
        }
    }

    /**
     * Update fixture status — PRD §23
     */
    suspend fun updateStatus(fixtureId: String, status: String) {
        adminFixtureDao.updateStatus(fixtureId, status)
    }

    /**
     * Postpone a match — PRD §22
     */
    suspend fun postponeMatch(fixtureId: String) {
        adminFixtureDao.updateStatus(fixtureId, "postponed")
    }

    /**
     * Cancel a match — PRD §22
     */
    suspend fun cancelMatch(fixtureId: String) {
        adminFixtureDao.updateStatus(fixtureId, "cancelled")
    }

    /**
     * Complete a match
     */
    suspend fun completeMatch(fixtureId: String) {
        adminFixtureDao.updateStatus(fixtureId, "completed")
    }

    /**
     * Delete a fixture — PRD §22
     */
    suspend fun deleteFixture(fixtureId: String) {
        val fixture = adminFixtureDao.findById(fixtureId) ?: return
        adminFixtureDao.deleteById(fixtureId)
        if (fixture.serverId != null) {
            syncManager.queueChange("fixture", fixtureId, "delete", mapOf(
                "tournamentId" to fixture.tournamentId,
                "serverId" to fixture.serverId.toString()
            ))
        }
    }

    /**
     * Get fixture count for a tournament
     */
    suspend fun getFixtureCount(tournamentId: String): Int =
        adminFixtureDao.getCountByTournament(tournamentId)

    /**
     * Get completed fixture count
     */
    suspend fun getCompletedCount(tournamentId: String): Int =
        adminFixtureDao.getCompletedCount(tournamentId)

    /**
     * Get a fixture as ScheduledFixture (for live scoring screens).
     */
    suspend fun getScheduledFixtureById(fixtureId: String): com.devwithguru.cricket.domain.model.ScheduledFixture? {
        val resolvedId = resolveOriginalFixtureId(fixtureId)
        val entity = fixtureDao.findById(resolvedId)
        if (entity != null) {
            return entity.toScheduledDomain()
        }
        val adminEntity = adminFixtureDao.findById(resolvedId) ?: return null
        return com.devwithguru.cricket.domain.model.ScheduledFixture(
            id = adminEntity.id,
            homeTeam = adminEntity.homeTeamName,
            awayTeam = adminEntity.awayTeamName,
            overs = 20,
            ballType = "Tennis",
            matchType = adminEntity.matchType,
            wickets = 10,
            venue = adminEntity.venue ?: "",
            date = adminEntity.scheduledDate ?: "",
            time = adminEntity.scheduledTime ?: "",
            status = adminEntity.status.replaceFirstChar { it.uppercase() }
        )
    }

    // ─── Backward-compatible methods for ScheduledFixture ───

    suspend fun resolveOriginalFixtureId(fixtureIdStr: String): String {
        val admin = adminFixtureDao.findById(fixtureIdStr)
        if (admin != null) return admin.id
        
        val allAdmin = adminFixtureDao.getAllAdminFixtures()
        val matchAdmin = allAdmin.find {
            it.id.hashCode().toString() == fixtureIdStr
        }
        if (matchAdmin != null) return matchAdmin.id
        
        return fixtureIdStr
    }

    suspend fun getAdminFixtureById(id: String): com.devwithguru.cricket.data.db.entity.AdminFixtureEntity? {
        val resolvedId = resolveOriginalFixtureId(id)
        return adminFixtureDao.findById(resolvedId)
    }

    suspend fun saveTossDetails(fixtureId: String, tossWinner: String, tossDecision: String) {
        val resolvedId = resolveOriginalFixtureId(fixtureId)
        val adminFixture = adminFixtureDao.findById(resolvedId)
        if (adminFixture != null) {
            val updated = adminFixture.copy(
                status = "toss_completed",
                tossWinner = tossWinner,
                tossDecision = tossDecision
            )
            adminFixtureDao.insert(updated)
        }
    }

    /**
     * Save a ScheduledFixture (live scoring data) to Room.
     * Maps ScheduledFixture fields to AdminFixtureEntity.
     */
    suspend fun saveFixture(fixture: com.devwithguru.cricket.domain.model.ScheduledFixture) {
        fixtureDao.insertFixture(fixture.toEntity())
        val adminEntity = adminFixtureDao.findById(fixture.id)
        if (adminEntity != null) {
            adminFixtureDao.insert(adminEntity.copy(status = fixture.status.lowercase()))
        }
    }

    /**
     * Update a ScheduledFixture in Room.
     */
    private fun extractTournamentId(fixtureId: String): String {
        if (!fixtureId.startsWith("fx_")) return ""
        val parts = fixtureId.split("_")
        if (parts.size >= 4 && (parts[1] == "t" || parts[1] == "at")) {
            return "${parts[1]}_${parts[2]}"
        }
        if (parts.size >= 3) {
            return parts[1]
        }
        return ""
    }

    private suspend fun updateTournamentStatus(tournamentId: String, status: String) {
        if (tournamentId.isBlank()) return
        val tournament = tournamentDao.findById(tournamentId)
        if (tournament != null) {
            tournamentDao.updateTournament(tournament.copy(status = status))
        }
    }

    suspend fun updateFixture(fixture: com.devwithguru.cricket.domain.model.ScheduledFixture) {
        saveFixture(fixture)
        
        val existingAdmin = adminFixtureDao.findById(fixture.id)
        val tournamentId = existingAdmin?.tournamentId ?: extractTournamentId(fixture.id)
        
        // Auto-advance tournament status based on match updates
        if (tournamentId.isNotBlank()) {
            val statusLower = fixture.status.lowercase()
            if (statusLower == "live" || statusLower == "active") {
                updateTournamentStatus(tournamentId, "active")
            } else if (statusLower == "completed") {
                updateTournamentStatus(tournamentId, "completed")
            }
        }
    }

    /**
     * Get all fixtures as ScheduledFixture list.
     */
    fun getAllFixtures(): Flow<List<com.devwithguru.cricket.domain.model.ScheduledFixture>> {
        // Use an empty tournamentId to get all — or use a broader query
        // For now, return empty flow (existing callers will use tournament-specific queries)
        return kotlinx.coroutines.flow.flow { emit(emptyList()) }
    }

    /**
     * Get fixtures by status as ScheduledFixture list.
     */
    fun getFixturesByStatus(status: String): Flow<List<com.devwithguru.cricket.domain.model.ScheduledFixture>> {
        return kotlinx.coroutines.flow.flow { emit(emptyList()) }
    }
}
