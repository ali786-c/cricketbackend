package com.devwithguru.cricket.data.repository

import com.devwithguru.cricket.data.api.ApiService
import com.devwithguru.cricket.data.api.toScheduledFixture
import com.devwithguru.cricket.domain.model.ScheduledFixture
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Repository that fetches match data from Laravel API
 * and falls back to Room database when offline.
 */
@Singleton
class MatchApiRepository @Inject constructor(
    private val apiService: ApiService,
    private val fixtureRepository: FixtureRepository,
    @Named("auth_token") private val authTokenProvider: () -> String?
) {
    private val _matchCache = MutableStateFlow<Map<String, ScheduledFixture>>(emptyMap())

    /**
     * Get match state from API, with Room fallback.
     * Returns a Flow that emits cached data first, then API data.
     */
    fun getMatchState(matchId: String): Flow<ScheduledFixture?> = flow {
        // 1. Emit cached data first (from Room)
        val cached = fixtureRepository.getScheduledFixtureById(matchId)
        emit(cached)

        // 2. Try fetching from API
        try {
            val token = authTokenProvider()
            val response = apiService.getMatchState(matchId, token)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    var fixture = body.data.toScheduledFixture(matchId)

                    // Merge with cached values to preserve correct team names, venue, date, time, and squad info
                    if (cached != null) {
                        val finalHome = if (fixture.homeTeam == "Team A" || fixture.homeTeam == "TBD" || fixture.homeTeam.isBlank()) {
                            cached.homeTeam
                        } else fixture.homeTeam

                        val finalAway = if (fixture.awayTeam == "Team B" || fixture.awayTeam == "TBD" || fixture.awayTeam.isBlank()) {
                            cached.awayTeam
                        } else fixture.awayTeam

                        fixture = fixture.copy(
                            homeTeam = finalHome,
                            awayTeam = finalAway,
                            venue = if (fixture.venue.isBlank()) cached.venue else fixture.venue,
                            date = if (fixture.date.isBlank()) cached.date else fixture.date,
                            time = if (fixture.time.isBlank()) cached.time else fixture.time
                        )
                    }

                    // Save to Room for offline access
                    fixtureRepository.saveFixture(fixture)

                    // Update in-memory cache
                    _matchCache.value = _matchCache.value + (matchId to fixture)

                    // Emit fresh API data
                    emit(fixture)
                }
            }
            // If API fails, the cached data we already emitted is the fallback
        } catch (e: Exception) {
            // Network error — cached data already emitted, that's our fallback
        }
    }

    /**
     * Force refresh from API (for pull-to-refresh or manual refresh).
     */
    suspend fun refreshMatchState(matchId: String): ScheduledFixture? {
        val cached = fixtureRepository.getScheduledFixtureById(matchId)
        return try {
            val token = authTokenProvider()
            val response = apiService.getMatchState(matchId, token)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    var fixture = body.data.toScheduledFixture(matchId)

                    // Merge with cached values to preserve correct team names, venue, date, time, and squad info
                    if (cached != null) {
                        val finalHome = if (fixture.homeTeam == "Team A" || fixture.homeTeam == "TBD" || fixture.homeTeam.isBlank()) {
                            cached.homeTeam
                        } else fixture.homeTeam

                        val finalAway = if (fixture.awayTeam == "Team B" || fixture.awayTeam == "TBD" || fixture.awayTeam.isBlank()) {
                            cached.awayTeam
                        } else fixture.awayTeam

                        fixture = fixture.copy(
                            homeTeam = finalHome,
                            awayTeam = finalAway,
                            venue = if (fixture.venue.isBlank()) cached.venue else fixture.venue,
                            date = if (fixture.date.isBlank()) cached.date else fixture.date,
                            time = if (fixture.time.isBlank()) cached.time else fixture.time
                        )
                    }

                    fixtureRepository.saveFixture(fixture)
                    _matchCache.value = _matchCache.value + (matchId to fixture)
                    fixture
                } else null
            } else null
        } catch (e: Exception) {
            // Fallback to Room
            cached
        }
    }

    /**
     * Get MVP data for a match.
     */
    suspend fun getMatchMvp(matchId: String): List<com.devwithguru.cricket.data.api.MvpPlayerData> {
        return try {
            val token = authTokenProvider()
            val response = apiService.getMatchMvp(matchId, token)
            if (response.isSuccessful) {
                response.body()?.data ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Poll match state (for live matches).
     * Returns new fixture if data changed, null otherwise.
     */
    suspend fun pollMatchState(matchId: String): ScheduledFixture? {
        val currentCached = _matchCache.value[matchId]
        val fresh = refreshMatchState(matchId)

        return if (fresh != null && fresh != currentCached) {
            fresh
        } else null
    }
}
