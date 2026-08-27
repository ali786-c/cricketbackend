package com.devwithguru.cricket.ui.feature.tournament

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
import com.devwithguru.cricket.data.api.AdminPlayerUser
import com.devwithguru.cricket.data.db.entity.AdminTeamEntity
import com.devwithguru.cricket.data.db.entity.AdminPlayerEntity
import com.devwithguru.cricket.data.db.entity.AdminFixtureEntity
import com.devwithguru.cricket.data.repository.AdminLocalRepository
import com.devwithguru.cricket.data.repository.AuthRepository
import com.devwithguru.cricket.data.repository.TournamentAdminRepository
import com.devwithguru.cricket.data.repository.DraftRepository
import com.devwithguru.cricket.data.repository.TournamentRepository
import com.devwithguru.cricket.data.repository.PlayerRepository
import com.devwithguru.cricket.data.repository.TeamRepository
import com.devwithguru.cricket.domain.model.Team
import com.devwithguru.cricket.data.sync.ConnectivityMonitor
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TournamentSetupViewModel @Inject constructor(
    private val localRepo: AdminLocalRepository,
    private val adminRepository: TournamentAdminRepository,
    private val draftRepository: DraftRepository,
    private val authRepository: AuthRepository,
    private val connectivityMonitor: ConnectivityMonitor,
    private val savedStateHandle: SavedStateHandle,
    private val tournamentRepository: TournamentRepository,
    private val playerRepository: PlayerRepository,
    private val teamRepository: TeamRepository
) : ViewModel() {

    // ─── Persisted UI State ────────────────────────────────
    val selectedTab: MutableStateFlow<Int> = MutableStateFlow(
        savedStateHandle.get<Int>("selected_tab") ?: 0
    )

    fun setSelectedTab(tab: Int) {
        selectedTab.value = tab
        savedStateHandle["selected_tab"] = tab
    }

    // hasDraft passed from navigation
    val hasDraft: Boolean = savedStateHandle.get<Boolean>("hasDraft") ?: true

    // ─── Tournament Status ─────────────────────────────────
    private val _tournamentStatus = MutableStateFlow("upcoming")
    val tournamentStatus: StateFlow<String> = _tournamentStatus

    // ─── Data from Room (OFFLINE-FIRST) ────────────────────
    private val _dbTeams = MutableStateFlow<List<AdminTeamEntity>>(emptyList())
    val dbTeams: StateFlow<List<AdminTeamEntity>> = _dbTeams

    private val _dbPlayers = MutableStateFlow<List<AdminPlayerEntity>>(emptyList())
    val dbPlayers: StateFlow<List<AdminPlayerEntity>> = _dbPlayers

    private val _dbFixtures = MutableStateFlow<List<AdminFixtureEntity>>(emptyList())
    val dbFixtures: StateFlow<List<AdminFixtureEntity>> = _dbFixtures

    // For UI compatibility — map Room entities to API models
    val teams: StateFlow<List<AdminTeamData>> = MutableStateFlow(emptyList())
    val players: StateFlow<List<AdminPlayerData>> = MutableStateFlow(emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private var hasLoadedOnce = false

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    // ─── Draft State ───────────────────────────────────────
    private val _draftPicks = MutableStateFlow<List<DraftPickData>>(emptyList())
    val draftPicks: StateFlow<List<DraftPickData>> = _draftPicks

    private val _draftRounds = MutableStateFlow<List<DraftRoundData>>(emptyList())
    val draftRounds: StateFlow<List<DraftRoundData>> = _draftRounds

    private val _draftStatus = MutableStateFlow<String?>(null)
    val draftStatus: StateFlow<String?> = _draftStatus

    private var _localDraftSetup: DraftSetupRequest? = null
    val localDraftSetup: DraftSetupRequest? get() = _localDraftSetup

    // ─── Load Data ─────────────────────────────────────────

    fun loadTournament(tournamentId: String) {
        // Step 1: Collect from Room (instant, offline-safe)
        viewModelScope.launch {
            localRepo.getTeams(tournamentId).collect { entities ->
                _dbTeams.value = entities
                // Map to API models for UI compatibility
                @Suppress("UNCHECKED_CAST")
                (teams as MutableStateFlow<List<AdminTeamData>>).value = entities.map { it.toApiModel() }
            }
        }
        viewModelScope.launch {
            localRepo.getPlayers(tournamentId).collect { entities ->
                _dbPlayers.value = entities
                @Suppress("UNCHECKED_CAST")
                (players as MutableStateFlow<List<AdminPlayerData>>).value = entities.map { it.toApiModel() }
            }
        }
        viewModelScope.launch {
            localRepo.getFixtures(tournamentId).collect { entities ->
                _dbFixtures.value = entities
            }
        }

        // Step 2: Background API refresh
        viewModelScope.launch {
            if (!hasLoadedOnce) _isLoading.value = true

            val token = authRepository.getRawToken()
            if (token != null) {
                try {
                    val teamsResult = adminRepository.getTeams(token, tournamentId)
                    teamsResult.onSuccess { apiTeams ->
                        apiTeams.forEach { apiTeam ->
                            val adminEntity = AdminTeamEntity(
                                id = apiTeam.id.toString(),
                                serverId = apiTeam.id,
                                tournamentId = tournamentId,
                                name = apiTeam.name ?: "Unknown",
                                shortName = apiTeam.short_name ?: "",
                                teamCode = apiTeam.unique_code,
                                captainUserId = apiTeam.active_captain?.user_id,
                                captainUserName = apiTeam.active_captain?.user?.name,
                                status = "approved",
                                syncStatus = "synced"
                            )
                            localRepo.saveTeam(adminEntity)
                            
                            teamRepository.saveTeam(Team(
                                id = apiTeam.id.toString(),
                                name = apiTeam.name ?: "",
                                shortName = apiTeam.short_name ?: "",
                                tournamentId = tournamentId,
                                playerCount = apiTeam.draft_picks_count ?: 0,
                                creatorId = apiTeam.active_captain?.user_id
                            ))
                        }
                    }
                } catch (_: Exception) { }

                try {
                    val playersResult = adminRepository.getPlayers(token, tournamentId)
                    playersResult.onSuccess { apiPlayers ->
                        apiPlayers.forEach { apiPlayer ->
                            val adminEntity = AdminPlayerEntity(
                                id = apiPlayer.id.toString(),
                                serverId = apiPlayer.id,
                                tournamentId = tournamentId,
                                playerName = apiPlayer.player_profile?.full_name ?: "Unknown",
                                role = apiPlayer.player_profile?.playing_role ?: "Batter",
                                city = apiPlayer.player_profile?.city ?: "",
                                status = apiPlayer.status ?: "pending",
                                syncStatus = "synced"
                            )
                            localRepo.savePlayer(adminEntity)
                            
                            playerRepository.registerPlayerWithTeam(
                                name = apiPlayer.player_profile?.full_name ?: "Unknown",
                                role = apiPlayer.player_profile?.playing_role ?: "Batter",
                                teamId = "",
                                isRegistered = true
                            )
                        }
                    }
                } catch (_: Exception) { }
            }

            _isLoading.value = false
            hasLoadedOnce = true
        }
    }

    // ─── Team Management (OFFLINE-FIRST via Room) ──────────

    fun createTeam(
        tournamentId: String,
        name: String,
        shortName: String? = null,
        captainName: String? = null,
        viceCaptainName: String? = null,
        managerName: String? = null
    ) {
        viewModelScope.launch {
            localRepo.createTeam(
                tournamentId = tournamentId,
                name = name,
                shortName = shortName ?: name.take(3).uppercase(),
                captainName = captainName,
                viceCaptainName = viceCaptainName,
                managerName = managerName
            )
            _successMessage.value = "Team \"$name\" created"
        }
    }

    fun deleteTeam(teamId: String) {
        viewModelScope.launch {
            localRepo.deleteTeam(teamId)
            _successMessage.value = "Team deleted"
        }
    }

    fun assignCaptain(tournamentId: String, teamId: String, userId: Int, userName: String = "Captain") {
        viewModelScope.launch {
            localRepo.assignCaptain(teamId, userId, userName)
            _successMessage.value = "Captain assigned"
        }
    }

    // ─── Player Management ─────────────────────────────────

    fun addPlayerManually(tournamentId: String, name: String, role: String, city: String) {
        viewModelScope.launch {
            localRepo.addPlayerManually(tournamentId, name, role, city)
            _successMessage.value = "Player \"$name\" added"
        }
    }

    fun approvePlayer(tournamentId: String, registrationId: String) {
        viewModelScope.launch {
            localRepo.approvePlayer(registrationId)
            _successMessage.value = "Player approved"
        }
    }

    fun rejectPlayer(tournamentId: String, registrationId: String) {
        viewModelScope.launch {
            localRepo.rejectPlayer(registrationId)
            _successMessage.value = "Player rejected"
        }
    }

    fun removePlayer(playerId: String) {
        viewModelScope.launch {
            localRepo.removePlayer(playerId)
            _successMessage.value = "Player removed"
        }
    }
    fun updateStatus(tournamentId: String, newStatus: String) {
        viewModelScope.launch {
            _tournamentStatus.value = newStatus
            localRepo.updateTournamentStatus(tournamentId, newStatus)
            val existing = tournamentRepository.getTournamentById(tournamentId)
            if (existing != null) {
                tournamentRepository.saveTournament(existing.copy(status = newStatus))
            }
            _successMessage.value = newStatus.replaceFirstChar { it.uppercase() }
        }
    }
    fun createFixture(tournamentId: String, request: CreateFixtureRequest) {
        viewModelScope.launch {
            val ht = _dbTeams.value.find { it.id.toLongOrNull() == request.home_team_id.toLong() }
            val at = _dbTeams.value.find { it.id.toLongOrNull() == request.away_team_id.toLong() }
            localRepo.createFixture(
                tournamentId, request.round_number ?: 1, request.round_name ?: "",
                request.match_number ?: 1, request.home_team_id.toString(), ht?.name ?: "",
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
    fun clearMessages() { _error.value = null; _successMessage.value = null }
}

private fun AdminTeamEntity.toApiModel() = AdminTeamData(
    id = serverId ?: id.hashCode(), name = name, short_name = shortName, unique_code = shortName,
    is_active = isActive, display_order = 0,
    active_captain = if (captainUserId != null) AdminCaptainData(
        user_id = captainUserId, assigned_at = System.currentTimeMillis().toString(),
        user = AdminCaptainUser(id = captainUserId, name = captainUserName ?: "", email = null)
    ) else null, draft_picks_count = 0
)
private fun AdminPlayerEntity.toApiModel() = AdminPlayerData(
    id = serverId ?: id.hashCode(), status = status,
    player_profile = AdminPlayerProfile(id = serverId ?: id.hashCode(), full_name = playerName, playing_role = role, city = city, user = null)
)
