package com.devwithguru.cricket.data.repository

import com.devwithguru.cricket.data.db.dao.TournamentDao
import com.devwithguru.cricket.data.mapper.toDomain
import com.devwithguru.cricket.data.mapper.toEntity
import com.devwithguru.cricket.domain.model.Tournament
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TournamentRepository @Inject constructor(
    private val tournamentDao: TournamentDao
) {
    fun getAllTournaments(): Flow<List<Tournament>> =
        tournamentDao.getAllTournaments().map { entities ->
            entities.map { it.toDomain() }
        }

    fun getTournamentsByStatus(status: String): Flow<List<Tournament>> =
        tournamentDao.getTournamentsByStatus(status).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getTournamentById(id: String): Tournament? =
        tournamentDao.findById(id)?.toDomain()

    fun observeTournament(id: String): Flow<Tournament?> =
        tournamentDao.observeById(id).map { it?.toDomain() }

    suspend fun saveTournament(tournament: Tournament) {
        tournamentDao.insertTournament(tournament.toEntity())
    }

    suspend fun updateTournament(tournament: Tournament) {
        tournamentDao.updateTournament(tournament.toEntity())
    }

    suspend fun updateTournamentTeamCount(id: String, count: Int) {
        tournamentDao.updateTeamCount(id, count)
    }

    suspend fun deleteTournament(tournament: Tournament) {
        tournamentDao.deleteTournament(tournament.toEntity())
    }

    suspend fun generateUniqueLocalId(): String {
        var uniqueId = ""
        do {
            val randomNum = (100000..999999).random()
            val candidateId = randomNum.toString()
            val exists = tournamentDao.findById(candidateId) != null
            if (!exists) {
                uniqueId = candidateId
            }
        } while (uniqueId.isEmpty())
        return uniqueId
    }
}
