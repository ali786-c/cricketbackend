package com.devwithguru.cricket.data.repository

import com.devwithguru.cricket.data.api.ApiService
import com.devwithguru.cricket.data.api.SearchData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Server-backed search repository.
 *
 * Search is ONLINE-ONLY by design: every query goes straight to the server so
 * results always reflect the latest data, including entities that were never
 * synced to the local Room database. There is intentionally no local fallback —
 * when the device is offline the caller shows an "offline" state instead.
 */
@Singleton
class SearchApiRepository @Inject constructor(
    private val apiService: ApiService
) {
    /**
     * Search the server.
     *
     * @param query search term (backend requires min 2 chars, max 100)
     * @param type  optional filter: "players" | "teams" | "tournaments" | "matches"
     *              or a comma-separated combination (e.g. "players,teams").
     *              Null searches every type.
     * @param limit max results per type (1-50, backend default 10)
     */
    suspend fun search(query: String, type: String? = null, limit: Int = 20): Result<SearchData> {
        return try {
            val response = apiService.search(query = query, type = type, limit = limit)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) {
                    Result.success(data)
                } else {
                    Result.failure(Exception("Empty search response"))
                }
            } else {
                Result.failure(Exception("Search failed (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
