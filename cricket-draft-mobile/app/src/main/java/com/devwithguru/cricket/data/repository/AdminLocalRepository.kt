package com.devwithguru.cricket.data.repository

import com.devwithguru.cricket.data.db.dao.AdminTeamDao
import com.devwithguru.cricket.data.db.dao.AdminPlayerDao
import com.devwithguru.cricket.data.db.dao.AdminFixtureDao
import com.devwithguru.cricket.data.db.dao.AdminDraftSetupDao
import com.devwithguru.cricket.data.db.entity.AdminTeamEntity
import com.devwithguru.cricket.data.db.entity.AdminPlayerEntity
import com.devwithguru.cricket.data.db.entity.AdminFixtureEntity
import com.devwithguru.cricket.data.db.entity.AdminDraftSetupEntity
import com.devwithguru.cricket.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core offline-first repository for admin tournament management.
 *
 * Strategy:
 * 1. WRITE to Room immediately (user sees instant result)
 * 2. Queue change for API sync
 * 3. When online, SyncManager pushes pending changes to server
 */
@Singleton
class AdminLocalRepository @Inject constructor(
    private val adminTeamDao: AdminTeamDao,
    private val adminPlayerDao: AdminPlayerDao,
    private val adminFixtureDao: AdminFixtureDao,
    private val adminDraftSetupDao: AdminDraftSetupDao,
    private val syncManager: SyncManager,
    private val authRepository: AuthRepository
) {
    // ─── Teams ──────────────────────────────────────────────

    fun getTeams(tournamentId: String): Flow<List<AdminTeamEntity>> =
        adminTeamDao.getByTournament(tournamentId)

    suspend fun createTeam(
        tournamentId: String,
        name: String,
        shortName: String,
        captainName: String? = null,
        viceCaptainName: String? = null,
        managerName: String? = null
    ): AdminTeamEntity {
        var uniqueId = ""
        do {
            val randomNum = (100000..999999).random()
            val candidateId = randomNum.toString()
            val exists = adminTeamDao.findById(candidateId) != null
            if (!exists) {
                uniqueId = candidateId
            }
        } while (uniqueId.isEmpty())
        val id = uniqueId
        val code = name.take(3).uppercase() + System.currentTimeMillis().toString().takeLast(4)
        val currentUserId = authRepository.getUserId()
        val creatorIdVal = if (currentUserId != -1) currentUserId else null
        val team = AdminTeamEntity(
            id = id,
            tournamentId = tournamentId,
            name = name,
            shortName = shortName.ifBlank { name.take(3).uppercase() },
            captainUserName = captainName,
            viceCaptainName = viceCaptainName,
            managerName = managerName,
            teamCode = code,
            status = "pending",
            syncStatus = "pending",
            creatorId = creatorIdVal
        )
        adminTeamDao.insert(team)
        syncManager.queueChange("admin_team", id, "create", buildMap {
            put("tournamentId", tournamentId)
            put("name", name)
            put("shortName", shortName)
            if (creatorIdVal != null) {
                put("creatorId", creatorIdVal)
            }
        })
        return team
    }

    suspend fun getTeamById(teamId: String): AdminTeamEntity? =
        adminTeamDao.findById(teamId)

    suspend fun getTeamByCode(code: String): AdminTeamEntity? =
        adminTeamDao.findByCode(code)

    suspend fun updateTeamStatus(teamId: String, status: String) {
        adminTeamDao.updateStatus(teamId, status)
    }

    suspend fun updateViceCaptain(teamId: String, name: String) {
        adminTeamDao.updateViceCaptain(teamId, name)
    }

    suspend fun updateManager(teamId: String, name: String) {
        adminTeamDao.updateManager(teamId, name)
    }

    suspend fun updatePlayerCount(teamId: String, count: Int) {
        adminTeamDao.updatePlayerCount(teamId, count)
    }

    suspend fun saveTeam(team: AdminTeamEntity) {
        adminTeamDao.insert(team)
    }

    suspend fun savePlayer(player: AdminPlayerEntity) {
        adminPlayerDao.insert(player)
    }

    suspend fun deleteTeam(teamId: String) {
        var team = adminTeamDao.findById(teamId)
        if (team == null) {
            val allTeams = adminTeamDao.getAllAdminTeams()
            team = allTeams.find {
                it.serverId?.toString() == teamId ||
                it.id.hashCode().toString() == teamId
            }
        }
        if (team != null) {
            adminTeamDao.deleteById(team.id)
            if (team.serverId != null) {
                syncManager.queueChange("admin_team", team.id, "delete", mapOf(
                    "tournamentId" to team.tournamentId, "serverId" to team.serverId
                ))
            }
        }
    }

    suspend fun assignCaptain(teamId: String, userId: Int, userName: String) {
        adminTeamDao.updateCaptain(teamId, userId, userName)
        val team = adminTeamDao.findById(teamId) ?: return
        syncManager.queueChange("captain", teamId, "assign", mapOf(
            "tournamentId" to team.tournamentId, "teamId" to teamId,
            "userId" to userId, "userName" to userName
        ))
    }

    suspend fun updateTeamServerId(localId: String, serverId: Int) {
        adminTeamDao.updateServerId(localId, serverId, "synced")
    }

    // ─── Players ────────────────────────────────────────────

    fun getPlayers(tournamentId: String): Flow<List<AdminPlayerEntity>> =
        adminPlayerDao.getByTournament(tournamentId)

    suspend fun addPlayerManually(tournamentId: String, name: String, role: String, city: String): AdminPlayerEntity {
        val id = "ap_${tournamentId}_${System.currentTimeMillis()}"
        val player = AdminPlayerEntity(
            id = id,
            tournamentId = tournamentId,
            playerName = name,
            role = role,
            city = city,
            status = "approved",
            isManuallyAdded = true,
            syncStatus = "pending"
        )
        adminPlayerDao.insert(player)
        syncManager.queueChange("admin_player", id, "create", mapOf(
            "tournamentId" to tournamentId, "name" to name,
            "role" to role, "city" to city
        ))
        return player
    }

    private suspend fun findAdminPlayer(playerId: String): AdminPlayerEntity? {
        val serverIdInt = playerId.toIntOrNull() ?: -1
        val players = adminPlayerDao.findByIdOrServerId(playerId, serverIdInt)
        if (players.isNotEmpty()) return players.first()
        return null
    }

    suspend fun approvePlayer(playerId: String) {
        val player = findAdminPlayer(playerId) ?: return
        adminPlayerDao.updateStatus(player.id, "approved")
        syncManager.queueChange("admin_player", player.id, "approve", mapOf(
            "tournamentId" to player.tournamentId, "playerId" to player.id
        ))
    }

    suspend fun rejectPlayer(playerId: String) {
        val player = findAdminPlayer(playerId) ?: return
        adminPlayerDao.updateStatus(player.id, "rejected")
        syncManager.queueChange("admin_player", player.id, "reject", mapOf(
            "tournamentId" to player.tournamentId, "playerId" to player.id
        ))
    }

    suspend fun removePlayer(playerId: String) {
        val player = findAdminPlayer(playerId) ?: return
        adminPlayerDao.deleteById(player.id)
    }

    // ─── Fixtures ───────────────────────────────────────────

    fun getFixtures(tournamentId: String): Flow<List<AdminFixtureEntity>> =
        adminFixtureDao.getByTournament(tournamentId)

    suspend fun createFixture(
        tournamentId: String, roundNumber: Int, roundName: String,
        matchNumber: Int, homeTeamId: String, homeTeamName: String,
        awayTeamId: String, awayTeamName: String,
        scheduledAt: String?, venue: String?, city: String?
    ): AdminFixtureEntity {
        val id = "af_${tournamentId}_${System.currentTimeMillis()}"
        val fixture = AdminFixtureEntity(
            id = id,
            tournamentId = tournamentId,
            roundNumber = roundNumber,
            roundName = roundName,
            matchNumber = matchNumber,
            homeTeamId = homeTeamId,
            homeTeamName = homeTeamName,
            awayTeamId = awayTeamId,
            awayTeamName = awayTeamName,
            scheduledAt = scheduledAt,
            venue = venue,
            city = city,
            syncStatus = "pending"
        )
        adminFixtureDao.insert(fixture)
        syncManager.queueChange("admin_fixture", id, "create", mapOf(
            "tournamentId" to tournamentId, "roundNumber" to roundNumber,
            "matchNumber" to matchNumber, "homeTeamId" to homeTeamId,
            "awayTeamId" to awayTeamId, "scheduledAt" to scheduledAt,
            "venue" to venue, "city" to city
        ))
        return fixture
    }

    suspend fun deleteFixture(fixtureId: String) {
        adminFixtureDao.deleteById(fixtureId)
    }

    // ─── Draft Setup ────────────────────────────────────────

    fun getDraftSetup(tournamentId: String): Flow<AdminDraftSetupEntity?> =
        adminDraftSetupDao.getByTournament(tournamentId)

    suspend fun saveDraftSetup(tournamentId: String, roundsJson: String) {
        adminDraftSetupDao.insert(AdminDraftSetupEntity(
            tournamentId = tournamentId,
            roundsJson = roundsJson,
            syncStatus = "pending"
        ))
        syncManager.queueChange("admin_draft_setup", tournamentId, "save", mapOf(
            "tournamentId" to tournamentId, "rounds" to roundsJson
        ))
    }

    // ─── Tournament Status ──────────────────────────────────

    suspend fun updateTournamentStatus(tournamentId: String, newStatus: String) {
        syncManager.queueChange("tournament_status", tournamentId, "update", mapOf(
            "tournamentId" to tournamentId, "status" to newStatus
        ))
    }

    // ─── Sync Support ───────────────────────────────────────

    suspend fun markTeamSynced(localId: String, serverId: Int) {
        adminTeamDao.updateServerId(localId, serverId, "synced")
    }

    suspend fun markPlayerSynced(localId: String, serverId: Int) {
        val player = adminPlayerDao.findById(localId) ?: return
        adminPlayerDao.insert(player.copy(serverId = serverId, syncStatus = "synced"))
    }

    suspend fun markFixtureSynced(localId: String, serverId: Int) {
        val fixture = adminFixtureDao.findById(localId) ?: return
        adminFixtureDao.insert(fixture.copy(serverId = serverId, syncStatus = "synced"))
    }
}
