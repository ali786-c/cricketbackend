import os

path = "app/src/main/java/com/devwithguru/cricket/ui/feature/tournament/TournamentSetupViewModel.kt"
content = """package com.devwithguru.cricket.ui.feature.tournament

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devwithguru.cricket.data.api.DraftPickData
import com.devwithguru.cricket.data.api.DraftRoundData
import com.devwithguru.cricket.data.api.CreateFixtureRequest
import com.devwithguru.cricket.data.api.DraftSetupRequest
import com.devwithguru.cricket.data.api.AdminTeamData
import com.devwithguru.cricket.data.api.AdminCaptainData
import com.devwithguru.cricket.data.api.AdminCaptainUser
import com.devwithguru.cricket.data.api.AdminPlayerData
import com.devwithguru.cricket.data.api.AdminPlayerProfile
import com.devwithguru.cricket.data.db.entity.AdminTeamEntity
import com.devwithguru.cricket.data.db.entity.AdminPlayerEntity
import com.devwithguru.cricket.data.db.entity.AdminFixtureEntity
import com.devwithguru.cricket.data.repository.AdminLocalRepository
import com.devwithguru.cricket.data.repository.AuthRepository
import com.devwithguru.cricket.data.repository.TournamentAdminRepository
import com.devwithguru.cricket.data.repository.DraftRepository
import com.devwithguru.cricket.data.sync.ConnectivityMonitor
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TournamentSetupViewModel @Inject constructor(
    private val localRepo: AdminLocalRepository,
    private val adminRepository: TournamentAdminRepository,
    private val draftRepository: DraftRepository,
    private val authRepository: AuthRepository,
    private val connectivityMonitor: ConnectivityMonitor,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val selectedTab: MutableStateFlow<Int> = MutableStateFlow(savedStateHandle.get<Int>("selected_tab") ?: 0)
    fun setSelectedTab(tab: Int) { selectedTab.value = tab; savedStateHandle["selected_tab"] = tab }
    val hasDraft: Boolean = savedStateHandle.get<Boolean>("hasDraft") ?: true

    private val _tournamentStatus = MutableStateFlow("draft")
    val tournamentStatus: StateFlow<String> = _tournamentStatus

    private val _dbTeams = MutableStateFlow<List<AdminTeamEntity>>(emptyList())
    val dbTeams: StateFlow<List<AdminTeamEntity>> = _dbTeams
    private val _dbPlayers = MutableStateFlow<List<AdminPlayerEntity>>(emptyList())
    val dbPlayers: StateFlow<List<AdminPlayerEntity>> = _dbPlayers
    private val _dbFixtures = MutableStateFlow<List<AdminFixtureEntity>>(emptyList())
    val dbFixtures: StateFlow<List<AdminFixtureEntity>> = _dbFixtures

    val teams: MutableStateFlow<List<AdminTeamData>> = MutableStateFlow(emptyList())
    val players: MutableStateFlow<List<AdminPlayerData>> = MutableStateFlow(emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private var hasLoadedOnce = false
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    private val _draftPicks = MutableStateFlow<List<DraftPickData>>(emptyList())
    val draftPicks: StateFlow<List<DraftPickData>> = _draftPicks
    private val _draftRounds = MutableStateFlow<List<DraftRoundData>>(emptyList())
    val draftRounds: StateFlow<List<DraftRoundData>> = _draftRounds
    private val _draftStatus = MutableStateFlow<String?>(null)
    val draftStatus: StateFlow<String?> = _draftStatus
    private var _localDraftSetup: DraftSetupRequest? = null
    val localDraftSetup: DraftSetupRequest? get() = _localDraftSetup

    fun loadTournament(tournamentId: String) {
        viewModelScope.launch { localRepo.getTeams(tournamentId).collect { e -> _dbTeams.value = e; teams.value = e.map { it.toApiModel() } } }
        viewModelScope.launch { localRepo.getPlayers(tournamentId).collect { e -> _dbPlayers.value = e; players.value = e.map { it.toApiModel() } } }
        viewModelScope.launch { localRepo.getFixtures(tournamentId).collect { e -> _dbFixtures.value = e } }
        viewModelScope.launch {
            if (!hasLoadedOnce) _isLoading.value = true
            val token = authRepository.getRawToken()
            if (token != null) {
                try { adminRepository.getTeams(token, tournamentId) } catch (_: Exception) { }
                try { adminRepository.getPlayers(token, tournamentId) } catch (_: Exception) { }
            }
            _isLoading.value = false; hasLoadedOnce = true
        }
    }

    fun createTeam(tournamentId: String, name: String, shortName: String? = null) {
        viewModelScope.launch {
            localRepo.createTeam(tournamentId, name, shortName ?: name.take(3).uppercase())
            _successMessage.value = "Team created"
        }
    }
    fun deleteTeam(teamId: String) {
        viewModelScope.launch { localRepo.deleteTeam(teamId); _successMessage.value = "Team deleted" }
    }
    fun assignCaptain(tournamentId: String, teamId: String, userId: Int, userName: String = "Captain") {
        viewModelScope.launch { localRepo.assignCaptain(teamId, userId, userName); _successMessage.value = "Captain assigned" }
    }
    fun addPlayerManually(tournamentId: String, name: String, role: String, city: String) {
        viewModelScope.launch { localRepo.addPlayerManually(tournamentId, name, role, city); _successMessage.value = "Player added" }
    }
    fun approvePlayer(tournamentId: String, registrationId: String) {
        viewModelScope.launch { localRepo.approvePlayer(registrationId); _successMessage.value = "Player approved" }
    }
    fun rejectPlayer(tournamentId: String, registrationId: String) {
        viewModelScope.launch { localRepo.rejectPlayer(registrationId); _successMessage.value = "Player rejected" }
    }
    fun removePlayer(playerId: String) {
        viewModelScope.launch { localRepo.removePlayer(playerId); _successMessage.value = "Player removed" }
    }
    fun updateStatus(tournamentId: String, newStatus: String) {
        viewModelScope.launch {
            _tournamentStatus.value = newStatus
            localRepo.updateTournamentStatus(tournamentId, newStatus)
            _successMessage.value = "Status: " + newStatus.replaceFirstChar { it.uppercase() }
        }
    }
    fun createFixture(tournamentId: String, request: CreateFixtureRequest) {
        viewModelScope.launch {
            val ht = _dbTeams.value.find { it.id.toLongOrNull() == request.home_team_id }
            val at = _dbTeams.value.find { it.id.toLongOrNull() == request.away_team_id }
            localRepo.createFixture(
                tournamentId, request.round_number, request.round_name, request.match_number,
                request.home_team_id.toString(), ht?.name ?: "",
                request.away_team_id.toString(), at?.name ?: "",
                request.scheduled_at, request.venue, request.city
            )
            _successMessage.value = "Fixture created"
        }
    }
    fun loadDraftState(tournamentId: String) {
        val token = authRepository.getRawToken() ?: return
        viewModelScope.launch {
            draftRepository.getAdminDraftState(token, tournamentId).onSuccess {
                _draftPicks.value = it.picks; _draftRounds.value = it.rounds; _draftStatus.value = it.status
            }
        }
    }
    fun saveDraftSetup(tournamentId: String, request: DraftSetupRequest) {
        viewModelScope.launch {
            localRepo.saveDraftSetup(tournamentId, Gson().toJson(request))
            _localDraftSetup = request; _successMessage.value = "Draft setup saved"
            val token = authRepository.getRawToken() ?: return@launch
            adminRepository.saveDraftSetup(token, tournamentId, request)
        }
    }
    fun clearMessages() { _error.value = null; _successMessage.value
