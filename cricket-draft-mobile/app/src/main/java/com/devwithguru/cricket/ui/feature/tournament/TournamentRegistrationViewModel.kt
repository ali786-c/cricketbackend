package com.devwithguru.cricket.ui.feature.tournament

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devwithguru.cricket.data.api.RegistrationStatusData
import com.devwithguru.cricket.data.api.RegistrationStoreData
import com.devwithguru.cricket.data.repository.AuthRepository
import com.devwithguru.cricket.data.repository.DraftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TournamentRegistrationViewModel @Inject constructor(
    val draftRepository: DraftRepository,
    val authRepository: AuthRepository
) : ViewModel() {

    private val _registrationStatus = MutableStateFlow<RegistrationStatusData?>(null)
    val registrationStatus: StateFlow<RegistrationStatusData?> = _registrationStatus

    private val _isRegistering = MutableStateFlow(false)
    val isRegistering: StateFlow<Boolean> = _isRegistering

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun checkRegistration(tournamentId: String) {
        viewModelScope.launch {
            val token = authRepository.getRawToken() ?: return@launch
            val result = draftRepository.getMyRegistration(token, tournamentId)
            result.onSuccess { _registrationStatus.value = it }
        }
    }

    fun register(tournamentId: String) {
        viewModelScope.launch {
            _isRegistering.value = true
            val token = authRepository.getRawToken() ?: run {
                _error.value = "Not authenticated"
                _isRegistering.value = false
                return@launch
            }
            val result = draftRepository.registerForTournament(token, tournamentId)
            result.onSuccess { data ->
                _registrationStatus.value = RegistrationStatusData(
                    id = data.id,
                    status = data.status,
                    submitted_at = null,
                    reviewed_at = null
                )
                _isRegistering.value = false
            }
            result.onFailure { e ->
                _error.value = e.message
                _isRegistering.value = false
            }
        }
    }
}
