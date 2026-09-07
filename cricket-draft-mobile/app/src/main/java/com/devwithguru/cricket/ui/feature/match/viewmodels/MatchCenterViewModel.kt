package com.devwithguru.cricket.ui.feature.match.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devwithguru.cricket.data.repository.FixtureRepository
import com.devwithguru.cricket.data.repository.MatchApiRepository
import com.devwithguru.cricket.data.sync.SyncManager
import com.devwithguru.cricket.domain.model.ScheduledFixture
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MatchCenterViewModel @Inject constructor(
    private val fixtureRepository: FixtureRepository,
    private val matchApiRepository: MatchApiRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _fixture = MutableStateFlow<ScheduledFixture?>(null)
    val fixture: StateFlow<ScheduledFixture?> = _fixture.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLive = MutableStateFlow(false)
    val isLive: StateFlow<Boolean> = _isLive.asStateFlow()

    private var pollingJob: Job? = null

    private val _isPreSyncing = MutableStateFlow(false)
    val isPreSyncing: StateFlow<Boolean> = _isPreSyncing

    /**
     * Load fixture data with pre-sync.
     * Flow: push local → pull server → load from Room → try API for live data.
     * This ensures mobile-created offline data reaches the server first.
     */
    fun loadFixture(matchId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Step 1: Pre-sync — push local data first, then pull server data
            if (syncManager.isOnline()) {
                _isPreSyncing.value = true
                try {
                    syncManager.pushPendingChanges()
                    syncManager.pullLatestData()
                } catch (_: Exception) { }
                _isPreSyncing.value = false
            }

            // Step 2: Load from Room (now has fresh server data)
            try {
                matchApiRepository.getMatchState(matchId).collect { fixture ->
                    if (fixture != null) {
                        _fixture.value = fixture
                        _isLive.value = fixture.status == "Live"

                        if (_isLive.value) {
                            startPolling(matchId)
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to Room only
                val roomFixture = fixtureRepository.getScheduledFixtureById(matchId)
                _fixture.value = roomFixture
                if (roomFixture != null) {
                    _isLive.value = roomFixture.status == "Live"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Force refresh from API.
     */
    fun refreshFixture(matchId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fixture = matchApiRepository.refreshMatchState(matchId)
                if (fixture != null) {
                    _fixture.value = fixture
                    _isLive.value = fixture.status == "Live"
                }
            } catch (e: Exception) {
                _error.value = "Failed to refresh: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Start polling for live match updates (every 10 seconds).
     */
    private fun startPolling(matchId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(POLL_INTERVAL_MS)
                try {
                    val updated = matchApiRepository.pollMatchState(matchId)
                    if (updated != null) {
                        _fixture.value = updated
                        if (updated.status != "Live") {
                            _isLive.value = false
                            break
                        }
                    } else {
                        // Fixture not found on server — stop polling
                        _isLive.value = false
                        break
                    }
                } catch (e: Exception) {
                    // Silently retry on next interval
                }
            }
        }
    }

    /**
     * Stop polling (when leaving screen).
     */
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * Update fixture in Room (for offline scoring).
     */
    fun updateFixture(fixture: ScheduledFixture) {
        viewModelScope.launch {
            fixtureRepository.updateFixture(fixture)
            _fixture.value = fixture
            // If status changed, update it on server too
            if (fixture.status == "Completed" || fixture.status == "Live") {
                try {
                    fixtureRepository.updateStatus(fixture.id, fixture.status)
                } catch (_: Exception) { }
            }
        }
    }

    /**
     * Update fixture status.
     */
    fun updateFixtureStatus(matchId: String, status: String) {
        viewModelScope.launch {
            fixtureRepository.updateStatus(matchId, status)
            val updated = fixtureRepository.getScheduledFixtureById(matchId)
            _fixture.value = updated
            _isLive.value = status == "Live"
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }

    companion object {
        private const val POLL_INTERVAL_MS = 10_000L // 10 seconds
    }
}
