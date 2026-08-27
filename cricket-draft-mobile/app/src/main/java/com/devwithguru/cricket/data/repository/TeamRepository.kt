package com.devwithguru.cricket.data.repository

import com.devwithguru.cricket.data.db.dao.TeamDao
import com.devwithguru.cricket.data.db.dao.AdminTeamDao
import com.devwithguru.cricket.data.db.entity.AdminTeamEntity
import com.devwithguru.cricket.data.mapper.toDomain
import com.devwithguru.cricket.data.mapper.toEntity
import com.devwithguru.cricket.domain.model.Team
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamRepository @Inject constructor(
    private val teamDao: TeamDao,
    private val adminTeamDao: AdminTeamDao
) {
    fun getAllTeams(): Flow<List<Team>> =
        teamDao.getAllTeams().map { entities ->
            entities.map { it.toDomain() }
        }

    fun getTeamsByTournament(tournamentId: String): Flow<List<Team>> =
        teamDao.getTeamsByTournament(tournamentId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getTeamById(id: String): Team? {
        val standardTeam = teamDao.findById(id)?.toDomain()
        if (standardTeam != null) return standardTeam

        val hash = id.toIntOrNull()
        val allAdminTeams = adminTeamDao.getAllAdminTeams()
        val matchedAdminTeam = allAdminTeams.find {
            it.id == id || (hash != null && (it.serverId == hash || it.id.hashCode() == hash))
        }
        return matchedAdminTeam?.toTeamDomain()
    }

    fun observeTeam(id: String): Flow<Team?> =
        teamDao.observeById(id).map { it?.toDomain() }

    suspend fun resolveOriginalTeamId(teamIdStr: String): String {
        val admin = adminTeamDao.findById(teamIdStr)
        if (admin != null) return admin.id
        
        val allAdmin = adminTeamDao.getAllAdminTeams()
        val matchAdmin = allAdmin.find {
            it.id.hashCode().toString() == teamIdStr ||
            it.serverId?.toString() == teamIdStr
        }
        if (matchAdmin != null) return matchAdmin.id
        
        return teamIdStr
    }

    suspend fun saveTeam(team: Team) {
        teamDao.insertTeam(team.toEntity())
        val adminTeam = adminTeamDao.findById(team.id)
        if (adminTeam != null) {
            adminTeamDao.insert(adminTeam.copy(
                name = team.name,
                shortName = team.shortName,
                captainUserName = team.captainName,
                viceCaptainName = team.viceCaptainName,
                wicketkeeperName = team.wicketkeeperName,
                managerName = team.managerName,
                status = team.status,
                playerCount = team.playerCount
            ))
        } else {
            val serverIdVal = team.id.toIntOrNull()
            adminTeamDao.insert(AdminTeamEntity(
                id = team.id,
                serverId = serverIdVal,
                tournamentId = team.tournamentId,
                name = team.name,
                shortName = team.shortName,
                captainUserName = team.captainName,
                viceCaptainName = team.viceCaptainName,
                wicketkeeperName = team.wicketkeeperName,
                managerName = team.managerName,
                status = team.status ?: "approved",
                playerCount = team.playerCount,
                teamCode = team.teamCode,
                syncStatus = "synced",
                creatorId = team.creatorId
            ))
        }
    }

    suspend fun updatePlayerCount(teamId: String, count: Int) {
        val standardTeam = teamDao.findById(teamId)
        if (standardTeam != null) {
            teamDao.updateTeam(standardTeam.copy(playerCount = count))
        }
        val adminTeam = adminTeamDao.findById(teamId)
        if (adminTeam != null) {
            adminTeamDao.updatePlayerCount(teamId, count)
        }
    }

    suspend fun updateTeam(team: Team) {
        saveTeam(team)
    }

    suspend fun deleteTeam(team: Team) {
        teamDao.deleteTeam(team.toEntity())
    }
}

fun AdminTeamEntity.toTeamDomain() = Team(
    id = id,
    name = name,
    shortName = shortName,
    tournamentId = tournamentId,
    logo = logo,
    captainName = captainUserName,
    viceCaptainName = viceCaptainName,
    managerName = managerName,
    wicketkeeperName = wicketkeeperName,
    status = status,
    playerCount = playerCount,
    teamCode = teamCode,
    syncStatus = syncStatus,
    creatorId = creatorId
)
