package com.devwithguru.cricket.data.repository

import com.devwithguru.cricket.data.api.ApiService
import com.devwithguru.cricket.data.api.TournamentData
import com.devwithguru.cricket.data.api.TournamentTeamData
import com.devwithguru.cricket.data.api.TournamentStandingData
import com.devwithguru.cricket.data.api.TournamentFixtureData2
import com.devwithguru.cricket.data.api.TeamSquadData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for tournament-related API calls.
 */
@Singleton
class TournamentApiRepository @Inject constructor(
    private val apiService: ApiService
) {
    /**
     * List all public tournaments.
     */
    suspend fun getTournaments(): Result<List<TournamentData>> {
        return try {
            val response = apiService.getTournaments()
            if (response.isSuccessful) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load tournaments"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * List my created tournaments.
     */
    suspend fun getAdminTournaments(token: String): Result<List<TournamentData>> {
        return try {
            val response = apiService.getAdminTournaments(token)
            if (response.isSuccessful) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load my tournaments"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get tournament details.
     */
    suspend fun getTournament(tournamentId: String): Result<TournamentData> {
        return try {
            val response = apiService.getTournament(tournamentId)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception("Failed to load tournament"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get tournament teams.
     */
    suspend fun getTournamentTeams(tournamentId: String): Result<List<TournamentTeamData>> {
        return try {
            val response = apiService.getTournamentTeams(tournamentId)
            if (response.isSuccessful) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load teams"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get tournament standings.
     */
    suspend fun getTournamentStandings(tournamentId: String): Result<List<TournamentStandingData>> {
        return try {
            val response = apiService.getTournamentStandings(tournamentId)
            if (response.isSuccessful) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load standings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get tournament fixtures.
     */
    suspend fun getTournamentFixtures(tournamentId: String): Result<List<TournamentFixtureData2>> {
        return try {
            val response = apiService.getTournamentFixtures(tournamentId)
            if (response.isSuccessful) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load fixtures"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get team squad.
     */
    suspend fun getTeamSquad(teamId: String): Result<TeamSquadData> {
        return try {
            val response = apiService.getTeamSquad(teamId)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception("Failed to load squad"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
