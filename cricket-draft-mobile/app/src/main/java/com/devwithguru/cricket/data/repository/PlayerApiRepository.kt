package com.devwithguru.cricket.data.repository

import com.devwithguru.cricket.data.api.ApiService
import com.devwithguru.cricket.data.api.PlayerStatsDetailData
import com.devwithguru.cricket.data.api.PlayerInsightsDetailData
import com.devwithguru.cricket.data.api.PlayerMatchData
import com.devwithguru.cricket.data.api.PlayerTeamData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for player-related API calls.
 */
@Singleton
class PlayerApiRepository @Inject constructor(
    private val apiService: ApiService
) {
    /**
     * Get player profile.
     */
    suspend fun getPlayerProfile(playerId: String, token: String? = null): Result<com.devwithguru.cricket.data.api.ProfileData> {
        return try {
            val response = apiService.getPlayerProfile(playerId, token)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception("Failed to load player profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get player stats.
     */
    suspend fun getPlayerStats(playerId: String, token: String? = null): Result<PlayerStatsDetailData> {
        return try {
            val response = apiService.getPlayerStats(playerId, token)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception("Failed to load player stats"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get player insights.
     */
    suspend fun getPlayerInsights(playerId: String, token: String? = null): Result<PlayerInsightsDetailData> {
        return try {
            val response = apiService.getPlayerInsights(playerId, token)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("No data"))
            } else {
                Result.failure(Exception("Failed to load player insights"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get player's match history.
     */
    suspend fun getPlayerMatches(playerId: String, token: String? = null): Result<List<PlayerMatchData>> {
        return try {
            val response = apiService.getPlayerMatches(playerId, token)
            if (response.isSuccessful) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load player matches"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get player's teams.
     */
    suspend fun getPlayerTeams(playerId: String, token: String? = null): Result<List<PlayerTeamData>> {
        return try {
            val response = apiService.getPlayerTeams(playerId, token)
            if (response.isSuccessful) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception("Failed to load player teams"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
