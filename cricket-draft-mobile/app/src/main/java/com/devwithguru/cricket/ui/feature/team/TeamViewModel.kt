package com.devwithguru.cricket.ui.feature.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devwithguru.cricket.data.api.SquadPlayerData
import com.devwithguru.cricket.data.repository.TeamApiRepository
import com.devwithguru.cricket.data.repository.TeamRepository
import com.devwithguru.cricket.data.repository.PlayerRepository
import com.devwithguru.cricket.data.repository.AdminLocalRepository
import com.devwithguru.cricket.data.repository.FixtureRepository
import com.devwithguru.cricket.data.repository.TournamentRepository
import com.devwithguru.cricket.domain.model.Team
import com.devwithguru.cricket.domain.model.RegisteredPlayer
import com.devwithguru.cricket.domain.model.Fixture
import com.devwithguru.cricket.domain.model.Tournament
import com.devwithguru.cricket.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeamViewModel @Inject constructor(
    private val teamRepository: TeamRepository,
    private val teamApiRepository: TeamApiRepository,
    private val playerRepository: PlayerRepository,
    private val localAdminRepository: AdminLocalRepository,
    private val fixtureRepository: FixtureRepository,
    private val tournamentRepository: TournamentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams

    private val _currentTeam = MutableStateFlow<Team?>(null)
    val currentTeam: StateFlow<Team?> = _currentTeam

    private val _squad = MutableStateFlow<List<SquadPlayerData>>(emptyList())
    val squad: StateFlow<List<SquadPlayerData>> = _squad

    private val _allRegisteredPlayers = MutableStateFlow<List<RegisteredPlayer>>(emptyList())
    val allRegisteredPlayers: StateFlow<List<RegisteredPlayer>> = _allRegisteredPlayers

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _fixtures = MutableStateFlow<List<Fixture>>(emptyList())
    val fixtures: StateFlow<List<Fixture>> = _fixtures

    private val _tournaments = MutableStateFlow<List<Tournament>>(emptyList())
    val tournaments: StateFlow<List<Tournament>> = _tournaments

    private val _isCreator = MutableStateFlow(false)
    val isCreator: StateFlow<Boolean> = _isCreator

    init {
        loadAllRegisteredPlayers()
    }

    fun loadAllRegisteredPlayers() {
        viewModelScope.launch {
            playerRepository.getAllPlayers().collect {
                _allRegisteredPlayers.value = it
            }
        }
    }

    /**
     * Load all teams from Room (always works offline).
     */
    fun loadAllTeams() {
        viewModelScope.launch {
            teamRepository.getAllTeams().collect {
                _teams.value = it
            }
        }
    }

    /**
     * Load teams by tournament from Room.
     */
    fun loadTeamsByTournament(tournamentId: String) {
        viewModelScope.launch {
            teamRepository.getTeamsByTournament(tournamentId).collect {
                _teams.value = it
            }
        }
    }

    /**
     * OFFLINE-FIRST: Load team details + squad.
     * 1. Show Room data instantly
     * 2. Background API refresh for squad
     */
    fun loadTeam(idStr: String) {
        // Step 1: Show Room data immediately
        viewModelScope.launch {
            val id = teamRepository.resolveOriginalTeamId(idStr)
            updateLocalPlayerCount(id)
            val team = teamRepository.getTeamById(id)
            _currentTeam.value = team
            val currentUserId = authRepository.getUserId()
            val isUserAdmin = authRepository.getUserRoles().any { it.equals("admin", ignoreCase = true) || it.equals("super_admin", ignoreCase = true) }
            
            var isTournamentOrganizer = false
            if (team != null && team.tournamentId.isNotBlank()) {
                val tournament = tournamentRepository.getTournamentById(team.tournamentId)
                _tournaments.value = if (tournament != null) listOf(tournament) else emptyList()
                isTournamentOrganizer = tournament?.organizerName?.isNotBlank() == true && 
                    tournament.organizerName.equals(authRepository.getUserName(), ignoreCase = true)
            } else {
                _tournaments.value = emptyList()
            }
            
            _isCreator.value = (currentUserId == -1 || isUserAdmin || team?.creatorId == null || team.creatorId == currentUserId || isTournamentOrganizer)
        }

        // Observe team matches
        viewModelScope.launch {
            val id = teamRepository.resolveOriginalTeamId(idStr)
            fixtureRepository.getFixturesByTeam(id).collect { list ->
                _fixtures.value = list
            }
        }

        // Step 1.5: Observe local squad players for this team and combine with team updates (for designations)
        viewModelScope.launch {
            val id = teamRepository.resolveOriginalTeamId(idStr)
            combine(
                playerRepository.getPlayersByTeam(id),
                _currentTeam
            ) { players, team ->
                players.map { p ->
                    SquadPlayerData(
                        tournament_player_id = p.id.toIntOrNull() ?: p.id.hashCode(),
                        player_name = p.name,
                        playing_role = p.role,
                        is_captain = (team != null && p.name == team.captainName),
                        is_vice_captain = (team != null && p.name == team.viceCaptainName),
                        is_wicketkeeper = (team != null && p.name == team.wicketkeeperName)
                    )
                }
            }.collect { mapped ->
                _squad.value = mapped
            }
        }

        // Step 2: Background API refresh
        viewModelScope.launch {
            val id = teamRepository.resolveOriginalTeamId(idStr)
            try {
                val result = teamApiRepository.getTeamPlayers(id)
                result.onSuccess { players ->
                    // Only overwrite if API returned a non-empty list (guard offline additions)
                    if (players.isNotEmpty()) {
                        _squad.value = players
                    }
                }
            } catch (_: Exception) {
                // Offline — Room data already showing
            }
        }
    }

    private fun resolveOriginalPlayerId(playerIdStr: String): String {
        val allPlayers = _allRegisteredPlayers.value
        val match = allPlayers.find {
            it.id == playerIdStr || 
            it.id.toIntOrNull()?.toString() == playerIdStr || 
            it.id.hashCode().toString() == playerIdStr
        }
        if (match != null) return match.id
        
        val squadPlayers = _squad.value
        val matchSquad = squadPlayers.find {
            it.tournament_player_id.toString() == playerIdStr
        }
        if (matchSquad != null) {
            val matchByName = allPlayers.find { it.name == matchSquad.player_name }
            if (matchByName != null) return matchByName.id
        }
        
        return playerIdStr
    }

    private suspend fun updateLocalPlayerCount(teamId: String) {
        val players = playerRepository.getPlayersByTeam(teamId).first()
        val count = players.size
        teamRepository.updatePlayerCount(teamId, count)
        val current = teamRepository.getTeamById(teamId)
        if (current != null) {
            _currentTeam.value = current
        }
    }

    fun addPlayerManually(name: String, role: String, teamIdStr: String) {
        viewModelScope.launch {
            val teamId = teamRepository.resolveOriginalTeamId(teamIdStr)
            playerRepository.registerPlayerWithTeam(name, role, teamId)
            updateLocalPlayerCount(teamId)
        }
    }

    fun addExistingPlayer(playerId: String, teamIdStr: String) {
        viewModelScope.launch {
            val teamId = teamRepository.resolveOriginalTeamId(teamIdStr)
            val resolvedId = resolveOriginalPlayerId(playerId)
            playerRepository.assignPlayerToTeam(resolvedId, teamId)
            updateLocalPlayerCount(teamId)
        }
    }

    fun removePlayerFromTeam(playerId: String) {
        viewModelScope.launch {
            val resolvedId = resolveOriginalPlayerId(playerId)
            val player = playerRepository.findById(resolvedId) ?: return@launch
            val teamId = player.teamId ?: return@launch
            val resolvedTeamId = teamRepository.resolveOriginalTeamId(teamId)
            playerRepository.assignPlayerToTeam(resolvedId, null)
            updateLocalPlayerCount(resolvedTeamId)
        }
    }

    fun setTeamCaptain(playerId: String, teamIdStr: String) {
        viewModelScope.launch {
            val teamId = teamRepository.resolveOriginalTeamId(teamIdStr)
            val resolvedId = resolveOriginalPlayerId(playerId)
            val player = playerRepository.findById(resolvedId) ?: return@launch
            val current = teamRepository.getTeamById(teamId) ?: return@launch
            
            // Toggle logic: if they are already captain, remove captain designation
            val newCaptainName = if (current.captainName == player.name) null else player.name
            val updatedTeam = current.copy(captainName = newCaptainName)
            teamRepository.saveTeam(updatedTeam)
            _currentTeam.value = updatedTeam

            // Sync to admin teams if applicable
            val adminTeam = localAdminRepository.getTeamById(teamId)
            if (adminTeam != null) {
                if (newCaptainName != null) {
                    val userId = player.id.toIntOrNull() ?: player.id.hashCode()
                    localAdminRepository.assignCaptain(teamId, userId, player.name)
                } else {
                    localAdminRepository.assignCaptain(teamId, 0, "")
                }
            }
        }
    }

    fun setTeamViceCaptain(playerId: String, teamIdStr: String) {
        viewModelScope.launch {
            val teamId = teamRepository.resolveOriginalTeamId(teamIdStr)
            val resolvedId = resolveOriginalPlayerId(playerId)
            val player = playerRepository.findById(resolvedId) ?: return@launch
            val current = teamRepository.getTeamById(teamId) ?: return@launch
            
            val newVcName = if (current.viceCaptainName == player.name) null else player.name
            val updatedTeam = current.copy(viceCaptainName = newVcName)
            teamRepository.saveTeam(updatedTeam)
            _currentTeam.value = updatedTeam
            
            val adminTeam = localAdminRepository.getTeamById(teamId)
            if (adminTeam != null) {
                localAdminRepository.updateViceCaptain(teamId, newVcName ?: "")
            }
        }
    }

    fun setTeamWicketkeeper(playerId: String, teamIdStr: String) {
        viewModelScope.launch {
            val teamId = teamRepository.resolveOriginalTeamId(teamIdStr)
            val resolvedId = resolveOriginalPlayerId(playerId)
            val player = playerRepository.findById(resolvedId) ?: return@launch
            val current = teamRepository.getTeamById(teamId) ?: return@launch
            
            val newWkName = if (current.wicketkeeperName == player.name) null else player.name
            val updatedTeam = current.copy(wicketkeeperName = newWkName)
            teamRepository.saveTeam(updatedTeam)
            _currentTeam.value = updatedTeam
        }
    }

    fun saveTeam(team: Team) {
        viewModelScope.launch {
            teamRepository.saveTeam(team)
            _currentTeam.value = team
        }
    }
}
