package com.devwithguru.cricket.data.repository

import com.devwithguru.cricket.data.api.ApiService
import com.devwithguru.cricket.data.api.AdminPlayerData
import com.devwithguru.cricket.data.api.AdminTeamData
import com.devwithguru.cricket.data.api.AssignCaptainRequest
import com.devwithguru.cricket.data.api.CreateTeamRequest
import com.devwithguru.cricket.data.api.CreateTournamentRequest
import com.devwithguru.cricket.data.api.TournamentData
import com.devwithguru.cricket.data.api.UpdateStatusRequest
import com.devwithguru.cricket.data.api.UpdateTournamentRequest
import com.devwithguru.cricket.data.api.CreateFixtureRequest
import com.devwithguru.cricket.data.api.DraftSetupRequest
import com.devwithguru.cricket.data.api.TournamentFixtureData2
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for admin tournament management API calls.
 */
@Singleton
class TournamentAdminRepository @Inject constructor(
    private val apiService: ApiService
) {
    private fun authHeader(token: String?) = token ?: ""

    // ─── Tournament CRUD ───────────────────────────────────

    suspend fun createTournament(token: String, request: CreateTournamentRequest): Result<TournamentData> {
        return try {
            val response = apiService.createTournament(authHeader(token), request)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to create tournament"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTournament(token: String, tournamentId: String, request: UpdateTournamentRequest): Result<TournamentData> {
        return try {
            val response = apiService.updateTournament(authHeader(token), tournamentId, request)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to update tournament"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStatus(token: String, tournamentId: String, status: String): Result<TournamentData> {
        return try {
            val response = apiService.updateTournamentStatus(authHeader(token), tournamentId, UpdateStatusRequest(status))
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to update status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Team Management ───────────────────────────────────

    suspend fun getTeams(token: String, tournamentId: String): Result<List<AdminTeamData>> {
        return try {
            val response = apiService.getAdminTeams(authHeader(token), tournamentId)
            if (response.isSuccessful) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load teams"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTeam(token: String, tournamentId: String, name: String, shortName: String? = null): Result<AdminTeamData> {
        return try {
            val response = apiService.createTeam(authHeader(token), tournamentId, CreateTeamRequest(name, shortName))
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to create team"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTeam(token: String, tournamentId: String, teamId: String): Result<Unit> {
        return try {
            val response = apiService.deleteTeam(authHeader(token), tournamentId, teamId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Failed to delete team"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Captain Management ────────────────────────────────

    suspend fun assignCaptain(token: String, tournamentId: String, teamId: String, userId: Int): Result<AdminTeamData> {
        return try {
            val response = apiService.assignCaptain(authHeader(token), tournamentId, teamId, AssignCaptainRequest(userId))
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to assign captain"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeCaptain(token: String, tournamentId: String, teamId: String): Result<AdminTeamData> {
        return try {
            val response = apiService.removeCaptain(authHeader(token), tournamentId, teamId)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to remove captain"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Player Management ─────────────────────────────────

    suspend fun getPlayers(token: String, tournamentId: String, status: String? = null): Result<List<AdminPlayerData>> {
        return try {
            val response = apiService.getAdminPlayers(authHeader(token), tournamentId, status)
            if (response.isSuccessful) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load players"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approvePlayer(token: String, tournamentId: String, registrationId: String): Result<AdminPlayerData> {
        return try {
            val response = apiService.approvePlayer(authHeader(token), tournamentId, registrationId)
            if (response.isSuccessful) {
                val data = response.body()
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to approve player"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectPlayer(token: String, tournamentId: String, registrationId: String): Result<AdminPlayerData> {
        return try {
            val response = apiService.rejectPlayer(authHeader(token), tournamentId, registrationId)
            if (response.isSuccessful) {
                val data = response.body()
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to reject player"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Fixture Management ─────────────────────────────────

    suspend fun getFixtures(token: String, tournamentId: String): Result<List<TournamentFixtureData2>> {
        return try {
            val response = apiService.getAdminFixtures(authHeader(token), tournamentId)
            if (response.isSuccessful) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load fixtures"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveDraftSetup(token: String, tournamentId: String, request: DraftSetupRequest): Result<Unit> {
        return try {
            val response = apiService.saveDraftSetup(authHeader(token), tournamentId, request)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Failed to save draft setup"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFixture(token: String, tournamentId: String, request: CreateFixtureRequest): Result<TournamentFixtureData2> {
        return try {
            val response = apiService.createFixture(authHeader(token), tournamentId, request)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to create fixture"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
