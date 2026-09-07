package com.devwithguru.cricket.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devwithguru.cricket.data.repository.PlayerRepository
import com.devwithguru.cricket.data.repository.TeamRepository
import com.devwithguru.cricket.data.repository.TournamentRepository
import com.devwithguru.cricket.data.repository.FixtureRepository
import com.devwithguru.cricket.domain.model.RegisteredPlayer
import com.devwithguru.cricket.domain.model.Team
import com.devwithguru.cricket.domain.model.Tournament
import com.devwithguru.cricket.domain.model.ScheduledFixture
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchItem(
    val id: String,
    val type: String, // "player", "team", "tournament", "match"
    val title: String,
    val subtitle: String
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val teamRepository: TeamRepository,
    private val tournamentRepository: TournamentRepository,
    private val fixtureRepository: FixtureRepository
) : ViewModel() {

    // Unified search results
    private val _searchResults = MutableStateFlow<List<SearchItem>>(emptyList())
    val searchResults: StateFlow<List<SearchItem>> = _searchResults

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // All data caches
    private val _allPlayers = MutableStateFlow<List<RegisteredPlayer>>(emptyList())
    private val _allTeams = MutableStateFlow<List<Team>>(emptyList())
    private val _allTournaments = MutableStateFlow<List<Tournament>>(emptyList())
    private val _allFixtures = MutableStateFlow<List<ScheduledFixture>>(emptyList())

    init {
        loadAllData()
    }

    private fun loadAllData() {
        viewModelScope.launch {
            _isLoading.value = true

            // Load all players
            playerRepository.getAllPlayers().collect { players ->
                _allPlayers.value = players
            }
        }

        viewModelScope.launch {
            // Load all teams
            teamRepository.getAllTeams().collect { teams ->
                _allTeams.value = teams
            }
        }

        viewModelScope.launch {
            // Load all tournaments
            tournamentRepository.getAllTournaments().collect { tournaments ->
                _allTournaments.value = tournaments
            }
        }

        viewModelScope.launch {
            // Load all fixtures
            fixtureRepository.getAllFixtures().collect { fixtures ->
                _allFixtures.value = fixtures
            }
        }
    }

    fun search(query: String) {
        _searchQuery.value = query

        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            val results = mutableListOf<SearchItem>()

            // Search players
            _allPlayers.value.forEach { player ->
                if (player.name.contains(query, ignoreCase = true) ||
                    player.role.contains(query, ignoreCase = true) ||
                    player.city?.contains(query, ignoreCase = true) == true
                ) {
                    val teamName = _allTeams.value.find { it.id == player.teamId }?.name ?: "No team"
                    results.add(
                        SearchItem(
                            id = player.id,
                            type = "player",
                            title = player.name,
                            subtitle = "$teamName • ${player.role}"
                        )
                    )
                }
            }

            // Search teams
            _allTeams.value.forEach { team ->
                if (team.name.contains(query, ignoreCase = true) ||
                    team.shortName.contains(query, ignoreCase = true) ||
                    team.teamCode.contains(query, ignoreCase = true)
                ) {
                    results.add(
                        SearchItem(
                            id = team.id,
                            type = "team",
                            title = team.name,
                            subtitle = "Active • ${team.playerCount} players"
                        )
                    )
                }
            }

            // Search tournaments
            _allTournaments.value.forEach { tournament ->
                if (tournament.name.contains(query, ignoreCase = true) ||
                    tournament.city?.contains(query, ignoreCase = true) == true ||
                    tournament.season.contains(query, ignoreCase = true)
                ) {
                    results.add(
                        SearchItem(
                            id = tournament.id,
                            type = "tournament",
                            title = tournament.name,
                            subtitle = "${tournament.city ?: "Unknown"} • ${tournament.status}"
                        )
                    )
                }
            }

            // Search fixtures/matches
            _allFixtures.value.forEach { fixture ->
                val homeTeam = _allTeams.value.find { it.id == fixture.homeTeam }
                val awayTeam = _allTeams.value.find { it.id == fixture.awayTeam }
                val homeName = homeTeam?.name ?: "Unknown"
                val awayName = awayTeam?.name ?: "Unknown"

                if (homeName.contains(query, ignoreCase = true) ||
                    awayName.contains(query, ignoreCase = true) ||
                    fixture.venue.contains(query, ignoreCase = true)
                ) {
                    val status = when (fixture.status.lowercase()) {
                        "live" -> "Live"
                        "completed" -> "Completed"
                        "toss_completed" -> "Toss Completed"
                        "scheduled" -> "Scheduled"
                        else -> fixture.status
                    }
                    results.add(
                        SearchItem(
                            id = fixture.id,
                            type = "match",
                            title = "$homeName vs $awayName",
                            subtitle = "$status • ${fixture.venue}"
                        )
                    )
                }
            }

            _searchResults.value = results.distinctBy { it.id }
            _isLoading.value = false
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    // Load recent searches from persistent storage (simplified - just returns recent items)
    fun getRecentSearches(): List<SearchItem> {
        val recent = mutableListOf<SearchItem>()

        // Add recent players (top 3)
        _allPlayers.value.take(3).forEach { player ->
            recent.add(
                SearchItem(
                    id = player.id,
                    type = "player",
                    title = player.name,
                    subtitle = player.role
                )
            )
        }

        // Add recent teams (top 3)
        _allTeams.value.take(3).forEach { team ->
            recent.add(
                SearchItem(
                    id = team.id,
                    type = "team",
                    title = team.name,
                    subtitle = "Team"
                )
            )
        }

        // Add recent tournaments (top 2)
        _allTournaments.value.take(2).forEach { tournament ->
            recent.add(
                SearchItem(
                    id = tournament.id,
                    type = "tournament",
                    title = tournament.name,
                    subtitle = tournament.status
                )
            )
        }

        return recent
    }
}
