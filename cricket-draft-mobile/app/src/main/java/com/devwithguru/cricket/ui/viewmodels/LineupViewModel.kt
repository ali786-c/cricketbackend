package com.devwithguru.cricket.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devwithguru.cricket.data.repository.PlayerRepository
import com.devwithguru.cricket.data.repository.FixtureRepository
import com.devwithguru.cricket.data.repository.TeamRepository
import com.devwithguru.cricket.data.repository.TournamentRepository
import com.devwithguru.cricket.data.sync.SyncManager
import com.devwithguru.cricket.ui.feature.match.toss.PlayerSelectable
import com.devwithguru.cricket.domain.model.RegisteredPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Squads for the Toss -> Lineup flow.
 *
 * Squad lists are loaded ONCE per match (one-shot Room reads) and afterwards
 * only ever APPENDED to. Earlier versions collected live Room Flows: every
 * insert into the players table - including the very insert that REGISTERS a
 * newly added player - re-fired the collector, and the re-queried list raced
 * (and lost against) the append, so freshly added players vanished from the
 * UI immediately. One-shot loads make the ViewModel the single writer.
 */
@HiltViewModel
class LineupViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val fixtureRepository: FixtureRepository,
    private val teamRepository: TeamRepository,
    private val tournamentRepository: TournamentRepository,
    private val syncManager: SyncManager
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

    var matchTournamentId: String = "0"
        private set

    private var squadsLoaded = false

    fun loadSquadsForMatch(matchId: String) {
        if (squadsLoaded) return // navigation relaunches fire LaunchedEffect again - don't reset/relaunch
        squadsLoaded = true
        _isLoading.value = true
        _homeSquad.value = emptyList()
        _awaySquad.value = emptyList()
        _squadSize.value = 11

        viewModelScope.launch {
            val adminFixture = fixtureRepository.getAdminFixtureById(matchId)
            val scheduledFixture = if (adminFixture == null) {
                fixtureRepository.getScheduledFixtureById(matchId)
            } else null

            // Resolve BOTH team IDs before loading squads. Custom-match fixtures
            // store team NAMES with blank/placeholder IDs, so prefer the stored
            // name and resolve it through the admin team registry (the Create
            // Match flow registers teams by name before any match exists).
            // Loading squads with an empty/unresolved key was the reason newly
            // registered players never showed up in the lineup.
            val homeKey = if (adminFixture != null) {
                adminFixture.homeTeamName.ifBlank { adminFixture.homeTeamId }
            } else {
                scheduledFixture?.homeTeam ?: ""
            }
            val awayKey = if (adminFixture != null) {
                adminFixture.awayTeamName.ifBlank { adminFixture.awayTeamId }
            } else {
                scheduledFixture?.awayTeam ?: ""
            }

            homeTeamId = teamRepository.resolveTeamIdByName(homeKey)
            awayTeamId = teamRepository.resolveTeamIdByName(awayKey)
            matchTournamentId = adminFixture?.tournamentId ?: "0"

            val tournament = adminFixture?.tournamentId
                ?.takeIf { it.isNotBlank() && it != "0" }
                ?.let { tournamentRepository.getTournamentById(it) }
            _squadSize.value = tournament?.squadSize
                ?: scheduledFixture?.wickets?.plus(1)
                ?: 11

            // One-shot loads - append-only from here on (see class KDoc).
            _homeSquad.value = playerRepository.getPlayersByTeamOnce(homeTeamId)
                .map { PlayerSelectable(it.id, it.name, it.role) }
            _awaySquad.value = playerRepository.getPlayersByTeamOnce(awayTeamId)
                .map { PlayerSelectable(it.id, it.name, it.role) }
            _isLoading.value = false
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

            // Append FIRST - after the one-shot load this ViewModel is the only
            // writer to the squad lists, so nothing can wipe this player anymore.
            if (forHomeTeam) {
                _homeSquad.value = _homeSquad.value + selectable
            } else {
                _awaySquad.value = _awaySquad.value + selectable
            }

            // Immediately sync or queue offline
            val payload = buildMap<String, String> {
                put("action", "create")
                put("name", name)
                put("role", role)
                put("tournamentId", matchTournamentId)
                if (targetTeamId.isNotBlank()) put("teamId", targetTeamId)
            }
            syncManager.queueChange("player", registered.id, "create", payload)
            syncManager.pushPendingChanges()

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
