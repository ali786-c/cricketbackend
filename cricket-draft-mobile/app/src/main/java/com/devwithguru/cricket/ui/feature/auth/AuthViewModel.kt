package com.devwithguru.cricket.ui.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devwithguru.cricket.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val userName: String = "",
    val userEmail: String = ""
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(
        isLoggedIn = authRepository.isLoggedIn(),
        userName = authRepository.getUserName(),
        userEmail = authRepository.getUserEmail()
    ))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Login with email/password.
     * OFFLINE-FIRST: If API fails but we have cached token, allow login.
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = authRepository.login(email, password)

            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        userName = response.data.name,
                        userEmail = response.data.email,
                        error = null
                    )
                },
                onFailure = { e ->
                    // OFFLINE FALLBACK: If we have a cached token, allow login anyway
                    // User can use app offline with previously cached data
                    if (authRepository.isLoggedIn()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            userName = authRepository.getUserName(),
                            userEmail = authRepository.getUserEmail(),
                            error = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message ?: "Login failed. Check your internet connection."
                        )
                    }
                }
            )
        }
    }

    /**
     * Check if we can auto-login from cache (no internet needed).
     */
    fun autoLoginFromCache(): Boolean {
        if (authRepository.isLoggedIn()) {
            _uiState.value = AuthUiState(
                isLoggedIn = true,
                userName = authRepository.getUserName(),
                userEmail = authRepository.getUserEmail()
            )
            return true
        }
        return false
    }

    fun logout() {
        authRepository.logout()
        _uiState.value = AuthUiState(isLoggedIn = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun getToken(): String? = authRepository.getToken()
}
