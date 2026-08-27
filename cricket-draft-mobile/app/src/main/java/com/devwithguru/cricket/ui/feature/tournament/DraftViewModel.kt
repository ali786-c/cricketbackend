package com.devwithguru.cricket.ui.feature.tournament

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devwithguru.cricket.data.api.DraftStateData
import com.devwithguru.cricket.data.repository.AuthRepository
import com.devwithguru.cricket.data.repository.DraftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DraftViewModel @Inject constructor(
    private val draftRepository: DraftRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _draftState = MutableStateFlow<DraftStateData?>(null)
    val draftState: StateFlow<DraftStateData?> = _draftState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    private var pollingJob: Job? = null
    private var timerJob: Job? = null

    private fun getToken(): String = authRepository.getRawToken() ?: ""

    /**
     * Load draft state and start polling.
     */
    fun loadDraft(tournamentId: String, isAdmin: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            refreshDraft(tournamentId, isAdmin)
            _isLoading.value = false
            startPolling(tournamentId, isAdmin)
        }
    }

    /**
     * Refresh draft state from API.
     */
    private suspend fun refreshDraft(tournamentId: String, isAdmin: Boolean) {
        try {
            val result = if (isAdmin) {
                draftRepository.getAdminDraftState(getToken(), tournamentId)
            } else {
                draftRepository.getCaptainDraftState(getToken(), tournamentId)
            }
            result.onSuccess { state ->
                _draftState.value = state
            }
            result.onFailure { e ->
                _error.value = e.message
            }
        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    /**
     * Poll draft state every 3 seconds for live updates.
     */
    private fun startPolling(tournamentId: String, isAdmin: Boolean) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(3000L)
                refreshDraft(tournamentId, isAdmin)

                // If draft is completed, stop polling
                if (_draftState.value?.status == "completed") break
            }
        }
    }

    /**
     * Start local timer countdown based on draft state.
     */
    fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _draftState.value ?: continue
                val expiresAt = current.timer.expires_at ?: continue
                // Timer is server-driven, polling will update the state
            }
        }
    }

    // ─── Admin Actions ─────────────────────────────────────

    fun startDraft(tournamentId: String) {
        viewModelScope.launch {
            val result = draftRepository.startDraft(getToken(), tournamentId)
            result.onSuccess { _draftState.value = it; _successMessage.value = "Draft started!" }
            result.onFailure { _error.value = it.message }
        }
    }

    fun adminSelectPlayer(tournamentId: String, pickNumber: Int, playerId: Int) {
        viewModelScope.launch {
            val result = draftRepository.adminSelectPlayer(getToken(), tournamentId, pickNumber, playerId)
            result.onSuccess { _draftState.value = it; _successMessage.value = "Player selected" }
            result.onFailure { _error.value = it.message }
        }
    }

    fun pauseDraft(tournamentId: String) {
        viewModelScope.launch {
            val result = draftRepository.pauseDraft(getToken(), tournamentId)
            result.onSuccess { _draftState.value = it; _successMessage.value = "Draft paused" }
            result.onFailure { _error.value = it.message }
        }
    }

    fun resumeDraft(tournamentId: String) {
        viewModelScope.launch {
            val result = draftRepository.resumeDraft(getToken(), tournamentId)
            result.onSuccess { _draftState.value = it; _successMessage.value = "Draft resumed" }
            result.onFailure { _error.value = it.message }
        }
    }

    fun extendTimer(tournamentId: String, seconds: Int = 60) {
        viewModelScope.launch {
            val result = draftRepository.extendTimer(getToken(), tournamentId, seconds)
            result.onSuccess { _draftState.value = it; _successMessage.value = "Timer extended" }
            result.onFailure { _error.value = it.message }
        }
    }

    fun skipPick(tournamentId: String) {
        viewModelScope.launch {
            val result = draftRepository.skipExpiredPick(getToken(), tournamentId)
            result.onSuccess { _draftState.value = it; _successMessage.value = "Pick skipped" }
            result.onFailure { _error.value = it.message }
        }
    }

    fun undoPick(tournamentId: String) {
        viewModelScope.launch {
            val result = draftRepository.undoPick(getToken(), tournamentId)
            result.onSuccess { _draftState.value = it; _successMessage.value = "Pick undone" }
            result.onFailure { _error.value = it.message }
        }
    }

    // ─── Captain Actions ───────────────────────────────────

    fun captainPick(tournamentId: String, tournamentPlayerId: Int) {
        viewModelScope.launch {
            val result = draftRepository.captainPick(getToken(), tournamentId, tournamentPlayerId)
            result.onSuccess { _draftState.value = it; _successMessage.value = "Player picked!" }
            result.onFailure { _error.value = it.message }
        }
    }

    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        timerJob?.cancel()
    }
}
