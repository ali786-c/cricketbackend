package com.devwithguru.cricket.data.repository

import com.devwithguru.cricket.data.db.dao.PlayerDao
import com.devwithguru.cricket.data.mapper.toDomain
import com.devwithguru.cricket.data.mapper.toEntity
import com.devwithguru.cricket.domain.model.RegisteredPlayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepository @Inject constructor(
    private val playerDao: PlayerDao
) {
    fun getAllPlayers(): Flow<List<RegisteredPlayer>> =
        playerDao.getAllPlayers().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun findById(id: String): RegisteredPlayer? =
        playerDao.findById(id)?.toDomain()

    suspend fun registerPlayer(name: String, role: String, isRegistered: Boolean = false, teamId: String? = null): RegisteredPlayer {
        val newId = (100000..999999).random().toString()
        val entity = com.devwithguru.cricket.data.db.entity.PlayerEntity(
            id = newId,
            playerProfileId = null,
            name = name,
            role = role,
            battingStyle = null,
            bowlingStyle = null,
            city = null,
            photoPath = null,
            isRegistered = isRegistered,
            teamId = teamId
        )
        playerDao.insertPlayer(entity)
        return entity.toDomain()
    }

    suspend fun registerPlayerWithTeam(name: String, role: String, teamId: String, isRegistered: Boolean = false): RegisteredPlayer {
        val newId = (100000..999999).random().toString()
        val entity = com.devwithguru.cricket.data.db.entity.PlayerEntity(
            id = newId,
            playerProfileId = null,
            name = name,
            role = role,
            battingStyle = null,
            bowlingStyle = null,
            city = null,
            photoPath = null,
            isRegistered = isRegistered,
            teamId = teamId
        )
        playerDao.insertPlayer(entity)
        return entity.toDomain()
    }

    fun getPlayersByTeam(teamId: String): Flow<List<RegisteredPlayer>> =
        playerDao.getPlayersByTeam(teamId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun assignPlayerToTeam(playerId: String, teamId: String?) {
        playerDao.updateTeamId(playerId, teamId)
    }

    suspend fun exists(id: String): Boolean = playerDao.exists(id)

    suspend fun getPlayersList(): List<RegisteredPlayer> =
        playerDao.getAllPlayers().first().map { it.toDomain() }
}
