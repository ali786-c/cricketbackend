package com.devwithguru.cricket.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devwithguru.cricket.data.repository.PlayerRepository
import com.devwithguru.cricket.data.repository.FixtureRepository
import com.devwithguru.cricket.data.repository.TeamRepository
import com.devwithguru.cricket.data.repository.TournamentRepository
import com.devwithguru.cricket.ui.feature.match.toss.PlayerSelectable
import com.devwithguru.cricket.domain.model.RegisteredPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LineupViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val fixtureRepository: FixtureRepository,
    private val teamRepository: TeamRepository,
    private val tournamentRepository: TournamentRepository
) : ViewModel() {

    private val _homeSquad = MutableStateFlow<List<PlayerSelectable>>(emptyList())
    val homeSquad: StateFlow<List<PlayerSelectable>> = _homeSquad

    private val _awaySquad = MutableStateFlow<List<PlayerSelectable>>(emptyList())
    val awaySquad: StateFlow<List<PlayerSelectable>> = _awaySquad

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchResult = MutableStateFlow<RegisteredPlayer?>(null)
    val searchResult: StateFlow<RegisteredPlayer?> = _searchResult

    private val _squadSize = MutableStateFlow(11)
    val squadSize: StateFlow<Int> = _squadSize

    var homeTeamId: String = ""
        private set
    var awayTeamId: String = ""
        private set

    fun loadSquadsForMatch(matchId: String) {
        _isLoading.value = true
        _homeSquad.value = emptyList()
        _awaySquad.value = emptyList()
        _squadSize.value = 11
        viewModelScope.launch {
            val adminFixture = fixtureRepository.getAdminFixtureById(matchId)
            if (adminFixture != null) {
                val originalHomeId = teamRepository.resolveOriginalTeamId(adminFixture.homeTeamId)
                val originalAwayId = teamRepository.resolveOriginalTeamId(adminFixture.awayTeamId)
                homeTeamId = originalHomeId
                awayTeamId = originalAwayId
                
                val tournament = tournamentRepository.getTournamentById(adminFixture.tournamentId)
                if (tournament != null) {
                    _squadSize.value = tournament.squadSize
                }
                
                playerRepository.getPlayersByTeam(originalHomeId).collect { list ->
                    _homeSquad.value = list.map { PlayerSelectable(it.id, it.name, it.role) }
                    _isLoading.value = false
                }
            } else {
                _isLoading.value = false
            }
        }
        viewModelScope.launch {
            val adminFixture = fixtureRepository.getAdminFixtureById(matchId)
            if (adminFixture != null) {
                val originalHomeId = teamRepository.resolveOriginalTeamId(adminFixture.homeTeamId)
                val originalAwayId = teamRepository.resolveOriginalTeamId(adminFixture.awayTeamId)
                homeTeamId = originalHomeId
                awayTeamId = originalAwayId
                playerRepository.getPlayersByTeam(originalAwayId).collect { list ->
                    _awaySquad.value = list.map { PlayerSelectable(it.id, it.name, it.role) }
                    _isLoading.value = false
                }
            } else {
                _isLoading.value = false
            }
        }
    }

    fun loadDefaultSquads() {
        viewModelScope.launch {
            val allPlayers = playerRepository.getPlayersList()
            val homePlayers = allPlayers.filter { it.id.startsWith("h") }.map {
                PlayerSelectable(it.id, it.name, it.role)
            }
            val awayPlayers = allPlayers.filter { it.id.startsWith("a") }.map {
                PlayerSelectable(it.id, it.name, it.role)
            }
            _homeSquad.value = homePlayers
            _awaySquad.value = awayPlayers
        }
    }

    fun searchPlayerById(id: String) {
        viewModelScope.launch {
            _searchResult.value = playerRepository.findById(id)
        }
    }

    fun clearSearchResult() {
        _searchResult.value = null
    }

    fun registerNewPlayer(name: String, role: String, forHomeTeam: Boolean, onComplete: (String) -> Unit = {}) {
        viewModelScope.launch {
            val targetTeamId = if (forHomeTeam) homeTeamId else awayTeamId
            val registered = playerRepository.registerPlayer(name, role, teamId = targetTeamId.takeIf { it.isNotBlank() })
            val selectable = PlayerSelectable(registered.id, registered.name, registered.role)
            if (forHomeTeam) {
                _homeSquad.value = _homeSquad.value + selectable
            } else {
                _awaySquad.value = _awaySquad.value + selectable
            }
            onComplete(registered.id)
        }
    }

    fun addPlayerToSquad(player: PlayerSelectable, forHomeTeam: Boolean) {
        if (forHomeTeam) {
            if (_homeSquad.value.none { it.id == player.id }) {
                _homeSquad.value = _homeSquad.value + player
            }
        } else {
            if (_awaySquad.value.none { it.id == player.id }) {
                _awaySquad.value = _awaySquad.value + player
            }
        }
    }
}
