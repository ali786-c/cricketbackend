package com.devwithguru.cricket.ui.feature.match.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devwithguru.cricket.data.repository.AdminLocalRepository
import com.devwithguru.cricket.data.repository.FixtureRepository
import com.devwithguru.cricket.data.sync.SyncManager
import com.devwithguru.cricket.domain.model.ScheduledFixture
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Create Match flow (standalone/custom matches).
 *
 * Everything created here is a GLOBAL entity:
 * - Teams are created as global teams (tournamentId "0") that hit
 *   POST /api/v1/custom/teams and appear in the SuperAdmin Teams tab immediately.
 * - Fixtures are queued as "admin_fixture" create changes that hit
 *   POST /api/v1/custom/fixtures (with team NAMES so the backend can
 *   auto-create missing teams) and appear in the SuperAdmin Fixtures tab.
 */
@HiltViewModel
class CreateMatchViewModel @Inject constructor(
    private val fixtureRepository: FixtureRepository,
    private val adminLocalRepository: AdminLocalRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _saveSuccess = MutableStateFlow<String?>(null)
    val saveSuccess: StateFlow<String?> = _saveSuccess

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    /**
     * Create a real global team with the given name and immediately attempt
     * to push it to the backend. The callback fires with the local team ID and
     * name so the caller can select it for a side without waiting for the network.
     */
    fun createTeam(name: String, location: String?, tournamentId: String? = null, onCreated: (id: String, teamName: String) -> Unit = { _, _ -> }) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                // Use the provided tournamentId, or "0" for standalone matches (global teams)
                val targetTournamentId = tournamentId.takeIf { !it.isNullOrBlank() } ?: "0"
                val team = adminLocalRepository.createTeam(
                    tournamentId = targetTournamentId,
                    name = trimmedName,
                    shortName = trimmedName.take(3).uppercase()
                )

                // Mirror into the standard teams table so team profiles/search work offline
                fixtureRepository.mirrorGlobalTeam(team)

                // Attempt instant push (no-ops safely when offline — the queue retains it)
                syncManager.pushPendingChanges()

                onCreated(team.id, team.name)
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Names of teams already known locally (synced from server + created on
     * this device) so the Create Match screen shows real teams, not a
     * hardcoded starter list.
     */
    suspend fun loadKnownTeamNames(): List<String> {
        return adminLocalRepository.getAllTeamNames()
    }

    /** Save the fixture locally AND queue it for backend sync (Save Fixture button). */
    fun saveFixture(fixture: ScheduledFixture) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                fixtureRepository.saveFixtureWithSync(fixture)
                fixtureRepository.pushPendingFixtureToServer(fixture.id)
                syncManager.pushPendingChanges()
                _saveSuccess.value = fixture.id
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Save + queue, attempt an immediate push, and ask the backend to create
     * the operational match once the fixture is synced (Start Match flow).
     */
    fun startFixture(fixture: ScheduledFixture, onReady: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                fixtureRepository.saveFixtureWithSync(fixture)
                fixtureRepository.pushPendingFixtureToServer(fixture.id)
                syncManager.pushPendingChanges()
                _saveSuccess.value = fixture.id
                onReady()
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearSaveSuccess() {
        _saveSuccess.value = null
    }
}
