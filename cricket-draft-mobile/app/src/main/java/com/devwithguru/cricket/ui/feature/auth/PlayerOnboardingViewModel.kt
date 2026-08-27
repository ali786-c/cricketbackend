package com.devwithguru.cricket.ui.feature.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devwithguru.cricket.data.api.UpdateProfileRequest
import com.devwithguru.cricket.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val isLoading: Boolean = false,
    val isCompleted: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PlayerOnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val profilePrefs: SharedPreferences by lazy {
        context.getSharedPreferences("cricket_profile_prefs", Context.MODE_PRIVATE)
    }

    /**
     * OFFLINE-FIRST: Save profile locally first, then try API.
     * - Local save is instant → user sees immediate success
     * - API call is best-effort → if offline, profile is still saved locally
     * - When back online, SyncManager will push the pending change
     */
    fun submitProfile(
        fullName: String,
        role: String,
        battingStyle: String,
        bowlingStyle: String,
        city: String,
        bio: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Step 1: Save locally FIRST (instant, always works)
            saveProfileLocally(fullName, role, battingStyle, bowlingStyle, city, bio)

            // Step 2: Try API (best-effort, may fail if offline)
            try {
                val request = UpdateProfileRequest(
                    full_name = fullName,
                    playing_role = role,
                    batting_style = battingStyle,
                    bowling_style = if (bowlingStyle == "None") null else bowlingStyle,
                    city = city.ifBlank { null },
                    bio = bio.ifBlank { null }
                )

                val result = authRepository.updateProfile(request)

                result.fold(
                    onSuccess = {
                        // API succeeded → mark as synced
                        profilePrefs.edit().putBoolean(KEY_PROFILE_SYNCED, true).apply()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isCompleted = true,
                            error = null
                        )
                    },
                    onFailure = {
                        // API failed (offline?) → still completed locally
                        // Mark as pending sync for SyncManager to push later
                        profilePrefs.edit().putBoolean(KEY_PROFILE_SYNCED, false).apply()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isCompleted = true,
                            error = null
                        )
                    }
                )
            } catch (_: Exception) {
                // Network error → still completed locally
                profilePrefs.edit().putBoolean(KEY_PROFILE_SYNCED, false).apply()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isCompleted = true,
                    error = null
                )
            }
        }
    }

    /**
     * Save profile data to SharedPreferences (local storage).
     */
    private fun saveProfileLocally(
        fullName: String,
        role: String,
        battingStyle: String,
        bowlingStyle: String,
        city: String,
        bio: String
    ) {
        profilePrefs.edit()
            .putString(KEY_FULL_NAME, fullName)
            .putString(KEY_ROLE, role)
            .putString(KEY_BATTING_STYLE, battingStyle)
            .putString(KEY_BOWLING_STYLE, bowlingStyle)
            .putString(KEY_CITY, city)
            .putString(KEY_BIO, bio)
            .putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
            .apply()
    }

    /**
     * Get locally saved profile (for offline display).
     */
    fun getLocalProfile(): Map<String, String?> {
        return mapOf(
            "fullName" to profilePrefs.getString(KEY_FULL_NAME, null),
            "role" to profilePrefs.getString(KEY_ROLE, null),
            "battingStyle" to profilePrefs.getString(KEY_BATTING_STYLE, null),
            "bowlingStyle" to profilePrefs.getString(KEY_BOWLING_STYLE, null),
            "city" to profilePrefs.getString(KEY_CITY, null),
            "bio" to profilePrefs.getString(KEY_BIO, null)
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    companion object {
        private const val KEY_FULL_NAME = "profile_full_name"
        private const val KEY_ROLE = "profile_role"
        private const val KEY_BATTING_STYLE = "profile_batting_style"
        private const val KEY_BOWLING_STYLE = "profile_bowling_style"
        private const val KEY_CITY = "profile_city"
        private const val KEY_BIO = "profile_bio"
        private const val KEY_LAST_UPDATED = "profile_last_updated"
        private const val KEY_PROFILE_SYNCED = "profile_synced"
    }
}
