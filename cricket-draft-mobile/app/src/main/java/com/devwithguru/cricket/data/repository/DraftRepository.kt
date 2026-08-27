package com.devwithguru.cricket.data.repository

import com.devwithguru.cricket.data.api.AdminSelectPlayerRequest
import com.devwithguru.cricket.data.api.ApiService
import com.devwithguru.cricket.data.api.CaptainPickRequest
import com.devwithguru.cricket.data.api.DraftStateData
import com.devwithguru.cricket.data.api.ExtendTimerRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for draft-related API calls.
 */
@Singleton
class DraftRepository @Inject constructor(
    private val apiService: ApiService
) {
    private fun authHeader(token: String) = "Bearer $token"

    // ─── Draft State ───────────────────────────────────────

    /**
     * Get draft state (admin view).
     */
    suspend fun getAdminDraftState(token: String, tournamentId: String): Result<DraftStateData> {
        return try {
            val response = apiService.getAdminDraftState(authHeader(token), tournamentId)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No draft data"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to load draft state"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get draft state (captain view).
     */
    suspend fun getCaptainDraftState(token: String, tournamentId: String): Result<DraftStateData> {
        return try {
            val response = apiService.getCaptainDraftState(authHeader(token), tournamentId)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No draft data"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to load draft state"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Admin Draft Controls ──────────────────────────────

    /**
     * Start the draft (admin).
     */
    suspend fun startDraft(token: String, tournamentId: String): Result<DraftStateData> {
        return try {
            val response = apiService.startDraft(authHeader(token), tournamentId)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to start draft"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin selects a player for a pick.
     */
    suspend fun adminSelectPlayer(token: String, tournamentId: String, pickNumber: Int, playerId: Int): Result<DraftStateData> {
        return try {
            val response = apiService.adminSelectPlayer(
                authHeader(token), tournamentId,
                AdminSelectPlayerRequest(pickNumber, playerId)
            )
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to select player"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pause the draft (admin).
     */
    suspend fun pauseDraft(token: String, tournamentId: String): Result<DraftStateData> {
        return try {
            val response = apiService.pauseDraft(authHeader(token), tournamentId)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to pause draft"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resume the draft (admin).
     */
    suspend fun resumeDraft(token: String, tournamentId: String): Result<DraftStateData> {
        return try {
            val response = apiService.resumeDraft(authHeader(token), tournamentId)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to resume draft"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extend the current pick timer (admin).
     */
    suspend fun extendTimer(token: String, tournamentId: String, seconds: Int): Result<DraftStateData> {
        return try {
            val response = apiService.extendDraftTimer(authHeader(token), tournamentId, ExtendTimerRequest(seconds))
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to extend timer"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Skip an expired pick (admin).
     */
    suspend fun skipExpiredPick(token: String, tournamentId: String): Result<DraftStateData> {
        return try {
            val response = apiService.skipExpiredPick(authHeader(token), tournamentId)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to skip pick"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Undo the latest pick (admin).
     */
    suspend fun undoPick(token: String, tournamentId: String): Result<DraftStateData> {
        return try {
            val response = apiService.undoDraftPick(authHeader(token), tournamentId)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to undo pick"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Captain Pick ──────────────────────────────────────

    /**
     * Captain makes a pick.
     */
    suspend fun captainPick(token: String, tournamentId: String, tournamentPlayerId: Int): Result<DraftStateData> {
        return try {
            val response = apiService.captainDraftPick(authHeader(token), tournamentId, CaptainPickRequest(tournamentPlayerId))
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to make pick"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Player Registration ───────────────────────────────

    /**
     * Get own registration status.
     */
    suspend fun getMyRegistration(token: String, tournamentId: String): Result<com.devwithguru.cricket.data.api.RegistrationStatusData?> {
        return try {
            val response = apiService.getMyRegistration(authHeader(token), tournamentId)
            if (response.isSuccessful) {
                Result.success(response.body()?.data)
            } else {
                Result.failure(Exception("Failed to check registration"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Register for a tournament.
     */
    suspend fun registerForTournament(token: String, tournamentId: String): Result<com.devwithguru.cricket.data.api.RegistrationStoreData> {
        return try {
            val response = apiService.registerForTournament(authHeader(token), tournamentId)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to register"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
