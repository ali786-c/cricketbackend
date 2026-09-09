package com.devwithguru.cricket.ui.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devwithguru.cricket.data.api.PlayerStatsDetailData
import com.devwithguru.cricket.data.api.PlayerInsightsDetailData
import com.devwithguru.cricket.data.api.PlayerMatchData
import com.devwithguru.cricket.data.api.PlayerTeamData
import com.devwithguru.cricket.data.repository.AuthRepository
import com.devwithguru.cricket.data.repository.PlayerApiRepository
import com.devwithguru.cricket.data.repository.PlayerRepository
import com.devwithguru.cricket.domain.model.RegisteredPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerProfileViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val playerApiRepository: PlayerApiRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _player = MutableStateFlow<RegisteredPlayer?>(null)
    val player: StateFlow<RegisteredPlayer?> = _player

    private val _stats = MutableStateFlow<PlayerStatsDetailData?>(null)
    val stats: StateFlow<PlayerStatsDetailData?> = _stats

    private val _insights = MutableStateFlow<PlayerInsightsDetailData?>(null)
    val insights: StateFlow<PlayerInsightsDetailData?> = _insights

    private val _matches = MutableStateFlow<List<PlayerMatchData>>(emptyList())
    val matches: StateFlow<List<PlayerMatchData>> = _matches

    private val _teams = MutableStateFlow<List<PlayerTeamData>>(emptyList())
    val teams: StateFlow<List<PlayerTeamData>> = _teams

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * Load player profile from API (or Room fallback).
     */
    fun loadPlayer(playerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val token = try {
                authRepository.getRawToken()?.let { "Bearer $it" }
            } catch (_: Exception) { null }

            // Fetch from API directly
            try {
                val result = playerApiRepository.getPlayerProfile(playerId, token)
                result.onSuccess { data ->
                    _player.value = RegisteredPlayer(
                        id = data.id.toString(),
                        name = data.full_name ?: "Unknown Player",
                        role = data.playing_role ?: "Player",
                        isRegistered = data.is_active,
                        teamId = null
                    )
                }.onFailure {
                    // Provide fallback if API fails
                    val isCurrentUser = playerId == authRepository.getPlayerProfileId().toString()
                    _player.value = RegisteredPlayer(
                        id = playerId,
                        name = if (isCurrentUser) authRepository.getUserName() else "Player Profile",
                        role = "Player",
                        isRegistered = true,
                        teamId = null
                    )
                }
            } catch (_: Exception) { }

            // Try each API call individually — one failure shouldn't kill the rest

            try {
                val result = playerApiRepository.getPlayerStats(playerId, token)
                result.onSuccess { _stats.value = it }
            } catch (_: Exception) { }

            try {
                val result = playerApiRepository.getPlayerInsights(playerId, token)
                result.onSuccess { _insights.value = it }
            } catch (_: Exception) { }

            try {
                val result = playerApiRepository.getPlayerMatches(playerId, token)
                result.onSuccess { _matches.value = it }
            } catch (_: Exception) { }

            try {
                val result = playerApiRepository.getPlayerTeams(playerId, token)
                result.onSuccess { _teams.value = it }
            } catch (_: Exception) {
                // Ignore API errors, use Room data
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load player from API.
     */
    fun loadPlayerProfile(playerId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val token = try {
                authRepository.getRawToken()?.let { "Bearer $it" }
            } catch (_: Exception) { null }

            try {
                val result = playerApiRepository.getPlayerProfile(playerId, token)
                result.onSuccess { data ->
                    _player.value = RegisteredPlayer(
                        id = data.id.toString(),
                        name = data.full_name ?: "Unknown Player",
                        role = data.playing_role ?: "Player",
                        isRegistered = data.is_active,
                        teamId = null
                    )
                }.onFailure {
                    val isCurrentUser = playerId == authRepository.getPlayerProfileId().toString()
                    _player.value = RegisteredPlayer(
                        id = playerId,
                        name = if (isCurrentUser) authRepository.getUserName() else "Player Profile",
                        role = "Player",
                        isRegistered = true,
                        teamId = null
                    )
                }
            } catch (_: Exception) { }
            
            _isLoading.value = false
        }
    }
}
