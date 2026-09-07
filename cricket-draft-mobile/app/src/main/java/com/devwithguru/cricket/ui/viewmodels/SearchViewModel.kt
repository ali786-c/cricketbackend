package com.devwithguru.cricket.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devwithguru.cricket.data.api.SearchData
import com.devwithguru.cricket.data.repository.SearchApiRepository
import com.devwithguru.cricket.data.sync.ConnectivityMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A single search result shown on the GlobalSearchScreen.
 * [id] is the SERVER id — result rows always point at server entities.
 */
data class SearchItem(
    val id: String,
    val type: String, // "player", "team", "tournament", "match"
    val title: String,
    val subtitle: String
)

/**
 * Filter chips on the search screen. [apiType] maps to the backend
 * `type` query param; null means "search everything".
 */
enum class SearchFilter(val label: String, val apiType: String?) {
    ALL("All", null),
    PLAYERS("Players", "players"),
    TEAMS("Teams", "teams"),
    TOURNAMENTS("Tournaments", "tournaments"),
    MATCHES("Matches", "matches")
}

/**
 * Online-only unified search.
 *
 * Every query hits GET /api/v1/search directly — results always come from the
 * server, so entities that were never synced locally are still findable.
 * When the device is offline the screen shows an explicit offline state; there
 * is intentionally NO local fallback.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchApiRepository: SearchApiRepository,
    private val connectivityMonitor: ConnectivityMonitor
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchItem>>(emptyList())
    val searchResults: StateFlow<List<SearchItem>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedFilter = MutableStateFlow(SearchFilter.ALL)
    val selectedFilter: StateFlow<SearchFilter> = _selectedFilter.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Drives the "you're offline — search needs internet" state on the screen. */
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Keep the offline flag in sync so the UI reacts when connectivity changes
        // (e.g. user toggles airplane mode while the search screen is open).
        viewModelScope.launch {
            connectivityMonitor.isOnline.collect { online ->
                _isOffline.value = !online
            }
        }
    }

    fun onQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        val trimmed = query.trim()
        if (trimmed.length < 2) {
            // Backend requires min 2 chars — don't fire a request that can't succeed.
            _searchResults.value = emptyList()
            _errorMessage.value = null
            _isLoading.value = false
            return
        }

        if (!connectivityMonitor.isCurrentlyOnline()) {
            // Online-only search: no local fallback, show offline state.
            _isOffline.value = true
            _searchResults.value = emptyList()
            _isLoading.value = false
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        searchJob = viewModelScope.launch {
            delay(300) // debounce keystrokes before hitting the server
            executeSearch(trimmed)
        }
    }

    fun selectFilter(filter: SearchFilter) {
        if (_selectedFilter.value == filter) return
        _selectedFilter.value = filter

        // Re-run the current query against the newly selected type.
        val trimmed = _searchQuery.value.trim()
        if (trimmed.length >= 2 && connectivityMonitor.isCurrentlyOnline()) {
            searchJob?.cancel()
            _isLoading.value = true
            _errorMessage.value = null
            searchJob = viewModelScope.launch { executeSearch(trimmed) }
        }
    }

    fun retry() {
        val trimmed = _searchQuery.value.trim()
        if (trimmed.length < 2) return
        searchJob?.cancel()
        _isLoading.value = true
        _errorMessage.value = null
        searchJob = viewModelScope.launch { executeSearch(trimmed) }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _errorMessage.value = null
        _isLoading.value = false
    }

    private suspend fun executeSearch(query: String) {
        val filter = _selectedFilter.value
        val result = searchApiRepository.search(query = query, type = filter.apiType)

        // Ignore stale responses (user kept typing or switched filter mid-flight).
        if (query != _searchQuery.value.trim() || filter != _selectedFilter.value) return

        result.fold(
            onSuccess = { data ->
                _searchResults.value = mapResults(data, filter)
                _errorMessage.value = null
            },
            onFailure = {
                _searchResults.value = emptyList()
                _errorMessage.value = if (!connectivityMonitor.isCurrentlyOnline()) {
                    "You're offline — search needs an internet connection."
                } else {
                    "Couldn't reach the server. Please try again."
                }
            }
        )
        _isLoading.value = false
    }

    private fun mapResults(data: SearchData, filter: SearchFilter): List<SearchItem> {
        val items = mutableListOf<SearchItem>()

        if (filter == SearchFilter.ALL || filter == SearchFilter.PLAYERS) {
            data.players.forEach { p ->
                items.add(
                    SearchItem(
                        id = p.id.toString(),
                        type = "player",
                        title = p.full_name ?: "Unknown player",
                        subtitle = listOfNotNull(
                            p.playing_role?.takeIf { it.isNotBlank() },
                            p.city?.takeIf { it.isNotBlank() },
                            p.unique_code?.takeIf { it.isNotBlank() }
                        ).joinToString(" • ").ifBlank { "Player" }
                    )
                )
            }
        }

        if (filter == SearchFilter.ALL || filter == SearchFilter.TEAMS) {
            data.teams.forEach { t ->
                items.add(
                    SearchItem(
                        id = t.id.toString(),
                        type = "team",
                        title = t.name ?: "Unknown team",
                        subtitle = listOfNotNull(
                            t.short_name?.takeIf { it.isNotBlank() && !it.equals(t.name, ignoreCase = true) },
                            t.unique_code?.takeIf { it.isNotBlank() }
                        ).joinToString(" • ").ifBlank { "Team" }
                    )
                )
            }
        }

        if (filter == SearchFilter.ALL || filter == SearchFilter.TOURNAMENTS) {
            data.tournaments.forEach { t ->
                items.add(
                    SearchItem(
                        // Backend binds tournament routes by SLUG (Tournament::getRouteKeyName),
                        // so deep-links must carry the slug — a numeric id would 404.
                        id = t.slug ?: t.id.toString(),
                        type = "tournament",
                        title = t.name ?: "Unknown tournament",
                        subtitle = listOfNotNull(
                            t.city?.takeIf { it.isNotBlank() },
                            t.status?.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() }
                        ).joinToString(" • ").ifBlank { "Tournament" }
                    )
                )
            }
        }

        if (filter == SearchFilter.ALL || filter == SearchFilter.MATCHES) {
            data.matches.forEach { m ->
                val status = when (m.status?.lowercase()) {
                    "live" -> "Live"
                    "completed" -> "Completed"
                    "scheduled" -> "Scheduled"
                    "toss_completed" -> "Toss Completed"
                    null -> ""
                    else -> m.status.replaceFirstChar { it.uppercase() }
                }
                items.add(
                    SearchItem(
                        id = m.id.toString(),
                        type = "match",
                        title = "${m.home_team ?: "Unknown"} vs ${m.away_team ?: "Unknown"}",
                        subtitle = listOfNotNull(
                            m.tournament?.takeIf { it.isNotBlank() },
                            status.takeIf { it.isNotBlank() }
                        ).joinToString(" • ").ifBlank { "Match" }
                    )
                )
            }
        }

        return items
    }

    override fun onCleared() {
        searchJob?.cancel()
        super.onCleared()
    }
}
