package com.devwithguru.cricket.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devwithguru.cricket.data.repository.FixtureRepository
import com.devwithguru.cricket.domain.model.ScheduledFixture
import com.devwithguru.cricket.data.sync.SyncManager
import com.devwithguru.cricket.data.sync.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for MainActivity — handles fixture lookups and updates
 * that happen in navigation callbacks (TossLineup, MatchCenter, etc.)
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val fixtureRepository: FixtureRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    init {
        // Push local changes first, then pull server data on app start
        viewModelScope.launch {
            syncManager.pushPendingChanges()
            syncManager.pullLatestData()
        }
    }

    private val _isPreSyncing = MutableStateFlow(false)
    val isPreSyncing: StateFlow<Boolean> = _isPreSyncing

    private val _preSyncMessage = MutableStateFlow<String?>(null)
    val preSyncMessage: StateFlow<String?> = _preSyncMessage

    private val _currentFixture = MutableStateFlow<ScheduledFixture?>(null)
    val currentFixture: StateFlow<ScheduledFixture?> = _currentFixture

    /**
     * Load fixture with pre-sync: push local data first, then pull server data.
     * This ensures mobile-created offline data reaches the server before we read it.
     */
    fun loadFixture(matchId: String) {
        viewModelScope.launch {
            // Pre-sync: push local changes first, then pull
            if (syncManager.isOnline()) {
                _isPreSyncing.value = true
                _preSyncMessage.value = "Syncing data..."
                syncManager.pushPendingChanges()
                syncManager.pullLatestData()
                _isPreSyncing.value = false
                _preSyncMessage.value = null
            }
            // Now read from Room (which has fresh server data)
            _currentFixture.value = fixtureRepository.getScheduledFixtureById(matchId)
        }
    }

    fun getFixture(matchId: String): ScheduledFixture? {
        val current = _currentFixture.value ?: return null
        val matches = current.id == matchId || current.id.hashCode().toString() == matchId
        return if (matches) current else null
    }

    fun updateFixture(fixture: ScheduledFixture) {
        viewModelScope.launch {
            fixtureRepository.updateFixture(fixture)
            _currentFixture.value = fixture
        }
    }

    fun saveTossDetails(matchId: String, winner: String, decision: String) {
        viewModelScope.launch {
            fixtureRepository.saveTossDetails(matchId, winner, decision)
        }
    }

    /**
     * Start match with pre-sync: push ALL local data first, then pull, then go live.
     * This ensures:
     * 1. Mobile-created fixtures/teams reach the server
     * 2. Web-created data is pulled down
     * 3. Both sides are in sync before scoring begins
     */
    fun startMatch(
        matchId: String,
        tossWinner: String,
        tossDecision: String,
        homeSquad: List<String>,
        awaySquad: List<String>,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            // Step 1: Pre-sync — push local, then pull server
            _isPreSyncing.value = true
            _preSyncMessage.value = "Syncing before going live..."
            val syncResult = syncManager.preSyncBeforeLiveScoring()
            _preSyncMessage.value = if (syncResult.success) "Synced ✓" else "Sync partial"

            // Step 2: Re-read fixture after sync (server may have updated it)
            val fixture = fixtureRepository.getScheduledFixtureById(matchId)

            // Step 3: Mark as Live and update
            fixture?.let { f ->
                f.status = "Live"
                f.homeSquad = homeSquad
                f.awaySquad = awaySquad
                f.tossWinner = tossWinner.takeIf { it.isNotBlank() } ?: f.tossWinner
                f.tossDecision = tossDecision.takeIf { it.isNotBlank() } ?: f.tossDecision
                fixtureRepository.updateFixture(f)
                _currentFixture.value = f
            }

            // Step 4: Queue the Live status for server sync
            if (fixture != null) {
                val adminFixture = fixtureRepository.getAdminFixtureById(fixture.id)
                val tournamentId = adminFixture?.tournamentId ?: ""
                syncManager.queueChange(
                    "fixture", fixture.id, "update",
                    mapOf("tournamentId" to tournamentId, "status" to "live")
                )
            }

            _isPreSyncing.value = false
            _preSyncMessage.value = null
            onComplete()
        }
    }
}
