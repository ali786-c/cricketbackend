package com.devwithguru.cricket.data.repository

import com.devwithguru.cricket.data.db.dao.AdminFixtureDao
import com.devwithguru.cricket.data.db.entity.AdminFixtureEntity
import com.devwithguru.cricket.data.mapper.toDomain
import com.devwithguru.cricket.data.mapper.toEntity
import com.devwithguru.cricket.data.mapper.toScheduledDomain
import com.devwithguru.cricket.data.sync.SyncManager
import com.devwithguru.cricket.data.api.ApiService
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
    private val teamDao: com.devwithguru.cricket.data.db.dao.TeamDao,
    private val syncManager: SyncManager,
    private val tournamentDao: com.devwithguru.cricket.data.db.dao.TournamentDao,
    private val apiService: com.devwithguru.cricket.data.api.ApiService,
    private val authRepository: AuthRepository,
    private val teamRepository: TeamRepository
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
     * Create an operational match from a fixture � server-side match creation
     */
    suspend fun createMatchFromFixture(
        tournamentId: String,
        fixtureId: String,
        token: String
    ): Result<com.devwithguru.cricket.data.api.CreateMatchFromFixtureData> {
        return try {
            val authHeader = "Bearer $token"
            val response = apiService.createMatchFromFixture(authHeader, tournamentId, fixtureId)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to create match"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
        val tournament = tournamentDao.findById(adminEntity.tournamentId)
        val defaultOvers = tournament?.oversPerInnings ?: 20
        return com.devwithguru.cricket.domain.model.ScheduledFixture(
            id = adminEntity.id,
            homeTeam = adminEntity.homeTeamName,
            awayTeam = adminEntity.awayTeamName,
            overs = defaultOvers,
            ballType = tournament?.ballType ?: "Tennis",
            matchType = adminEntity.matchType,
            wickets = tournament?.wicketsPerTeam ?: 10,
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
     * Mirror a global (custom-match) team into the standard teams table so it
     * appears in team profiles/search offline before the next server pull.
     */
    suspend fun mirrorGlobalTeam(team: com.devwithguru.cricket.data.db.entity.AdminTeamEntity) {
        teamDao.insertTeam(
            com.devwithguru.cricket.data.db.entity.TeamEntity(
                id = team.id,
                serverId = team.serverId,
                name = team.name,
                shortName = team.shortName,
                tournamentId = team.tournamentId,
                teamCode = team.teamCode,
                creatorId = team.creatorId,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Save a ScheduledFixture to Room AND register it as a real admin fixture
     * queued for backend sync. Standalone matches use tournamentId "0" so the
     * backend stores them as custom fixtures (visible in SuperAdmin → Fixtures).
     * The payload carries team NAMES so the backend auto-creates missing teams.
     */
    suspend fun saveFixtureWithSync(fixture: com.devwithguru.cricket.domain.model.ScheduledFixture) {
        // 1. Local scoring record (existing offline scoring behavior)
        fixtureDao.insertFixture(fixture.toEntity())

        // 2. Admin fixture record for sync (create or update)
        val existing = adminFixtureDao.findById(fixture.id)
        val dateIso = toIsoDate(fixture.date)

        // Resolve the local team IDs from the stored NAMES so the lineup
        // screen can link players to the right squad. The sync payload below
        // deliberately keeps "0" + names: the backend auto-creates global
        // teams by name, and sending a local-only numeric ID would fail its
        // server-side team validation (422).
        val resolvedHomeId = teamRepository.resolveTeamIdByName(fixture.homeTeam)
            .takeIf { !it.equals(fixture.homeTeam.trim(), ignoreCase = true) }
            ?: existing?.homeTeamId.takeUnless { it.isNullOrBlank() }
            ?: ""
        val resolvedAwayId = teamRepository.resolveTeamIdByName(fixture.awayTeam)
            .takeIf { !it.equals(fixture.awayTeam.trim(), ignoreCase = true) }
            ?: existing?.awayTeamId.takeUnless { it.isNullOrBlank() }
            ?: ""

        val adminEntity = if (existing != null) {
            existing.copy(
                homeTeamId = resolvedHomeId.ifBlank { existing.homeTeamId },
                awayTeamId = resolvedAwayId.ifBlank { existing.awayTeamId },
                homeTeamName = fixture.homeTeam,
                awayTeamName = fixture.awayTeam,
                scheduledDate = dateIso,
                scheduledTime = fixture.time,
                scheduledAt = "$dateIso ${fixture.time}",
                venue = fixture.venue,
                status = fixture.status.lowercase(),
                updatedAt = System.currentTimeMillis()
            )
        } else {
            com.devwithguru.cricket.data.db.entity.AdminFixtureEntity(
                id = fixture.id,
                tournamentId = "0",
                roundNumber = 1,
                roundName = "Custom Match",
                homeTeamId = resolvedHomeId,
                awayTeamId = resolvedAwayId,
                homeTeamName = fixture.homeTeam,
                awayTeamName = fixture.awayTeam,
                scheduledDate = dateIso,
                scheduledTime = fixture.time,
                scheduledAt = "$dateIso ${fixture.time}",
                venue = fixture.venue,
                status = fixture.status.lowercase(),
                syncStatus = "pending"
            )
        }
        adminFixtureDao.insert(adminEntity)

        // 3. Queue for backend sync (create once, then only updates)
        if (existing?.serverId == null) {
            syncManager.queueChange("admin_fixture", fixture.id, "create", mapOf(
                "tournamentId" to "0",
                "roundNumber" to 1,
                "roundName" to "Custom Match",
                "matchNumber" to 1,
                "homeTeamId" to "0",
                "awayTeamId" to "0",
                "homeTeamName" to fixture.homeTeam,
                "awayTeamName" to fixture.awayTeam,
                "scheduledDate" to dateIso,
                "scheduledTime" to fixture.time,
                "venue" to fixture.venue
            ))
        }
    }

    /**
     * Parse the human-readable date used by the Create Match screen
 * (e.g. "19 Aug 2026") into an ISO "yyyy-MM-dd" string the backend
 * validation accepts. Falls back to today's date when parsing fails so a
 * whole sync batch is never rejected over one bad date.
 */
    fun toIsoDate(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return todayIso()
        if (Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(trimmed)) return trimmed
        val monthMap = mapOf(
            "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4,
            "may" to 5, "jun" to 6, "jul" to 7, "aug" to 8,
            "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12
        )
        val parts = trimmed.lowercase()
            .replace("-", " ").replace(",", " ")
            .split(Regex("\\s+")).filter { it.isNotBlank() }
        val day = parts.firstOrNull { it.toIntOrNull() != null }?.toIntOrNull()
        val month = parts.firstOrNull { monthMap.containsKey(it.take(3)) }?.let { monthMap[it.take(3)] }
        val year = parts.lastOrNull { it.length == 4 && it.toIntOrNull() != null }?.toIntOrNull()
        return if (day != null && month != null && year != null) {
            String.format("%04d-%02d-%02d", year, month, day)
        } else {
            todayIso()
        }
    }

    private fun todayIso(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

    /**
     * Push a pending fixture to the backend immediately (bypassing queue order)
     * so an operational match can be created on Start Match. Maps the returned
     * server ID back onto the local record for dedup.
     */
    suspend fun pushPendingFixtureToServer(fixtureId: String): Boolean {
        val fixture = adminFixtureDao.findById(fixtureId) ?: return false
        if (fixture.serverId != null) return true // already synced
        // Only standalone/custom fixtures are auto-pushed here; tournament
        // fixtures continue through the normal SyncManager queue path.
        val tournamentId0 = fixture.tournamentId
        if (!(tournamentId0.isBlank() || tournamentId0 == "0" || tournamentId0 == "custom")) return false
        val token = authRepository.getRawToken() ?: return false
        val authHeader = "Bearer $token"

        val dateIso = toIsoDate(fixture.scheduledDate ?: "")
        val scheduledAt = "${dateIso}T${(fixture.scheduledTime ?: "00:00").take(5)}:00.000000Z"
        val body = com.devwithguru.cricket.data.api.CreateFixtureRequest(
            home_team_id = 0,
            away_team_id = 0,
            home_team_name = fixture.homeTeamName.ifBlank { "Home Team" },
            away_team_name = fixture.awayTeamName.ifBlank { "Away Team" },
            round_number = fixture.roundNumber,
            round_name = fixture.roundName.ifBlank { "Custom Match" },
            match_number = fixture.matchNumber,
            scheduled_at = scheduledAt,
            venue = fixture.venue,
            city = fixture.city
        )

        val response = apiService.createCustomFixture(authHeader, body)

        return if (response.isSuccessful) {
            response.body()?.data?.id?.let { serverId ->
                adminFixtureDao.updateServerIdAndSync(fixture.id, serverId, "synced")
            }
            true
        } else false
    }

    /**
     * Ask the backend to create the operational match for a synced fixture.
     * Backend is idempotent (rejects duplicate match creation), so the call is
     * safe to repeat. Standalone fixtures (tournamentId "0") use the custom route.
     */
    suspend fun createOperationalMatchIfNeeded(fixtureId: String) {
        val fixture = adminFixtureDao.findById(fixtureId) ?: return
        val serverFixtureId = fixture.serverId ?: return
        val token = authRepository.getRawToken() ?: return
        val authHeader = "Bearer $token"
        val tournamentId = fixture.tournamentId
        if (tournamentId.isBlank() || tournamentId == "0" || tournamentId == "custom") {
            apiService.createCustomMatch(authHeader, serverFixtureId.toString())
        } else {
            apiService.createMatchFromFixture(authHeader, tournamentId, serverFixtureId.toString())
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
