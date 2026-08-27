package com.devwithguru.cricket.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.devwithguru.cricket.data.api.ApiService
import com.devwithguru.cricket.data.api.LoginRequest
import com.devwithguru.cricket.data.api.LoginResponse
import com.devwithguru.cricket.data.api.UpdateProfileRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles authentication: login, logout, token storage, profile updates.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ─── Token Management ──────────────────────────────────

    fun getToken(): String? {
        val token = prefs.getString(KEY_TOKEN, null)
        return if (!token.isNullOrBlank()) "Bearer $token" else null
    }

    fun getRawToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    fun isLoggedIn(): Boolean = !getRawToken().isNullOrBlank()

    // ─── User Data ─────────────────────────────────────────

    fun saveUserData(id: Int, name: String, email: String, roles: List<String>) {
        prefs.edit()
            .putInt(KEY_USER_ID, id)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .putStringSet(KEY_USER_ROLES, roles.toSet())
            .apply()
    }

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)
    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""
    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""
    fun getUserRoles(): Set<String> = prefs.getStringSet(KEY_USER_ROLES, emptySet()) ?: emptySet()

    // ─── API Calls ─────────────────────────────────────────

    /**
     * Login with email/password. Returns LoginResponse on success.
     */
    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(
                LoginRequest(email = email, password = password)
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // Save token
                    saveToken(body.token)
                    // Save user data
                    saveUserData(body.data.id, body.data.name, body.data.email, body.data.roles)
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty response from server"))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Login failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get current user from API.
     */
    suspend fun getMe(): Result<com.devwithguru.cricket.data.api.UserData> {
        return try {
            val token = getToken() ?: return Result.failure(Exception("Not logged in"))
            val response = apiService.getMe(token)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    saveUserData(body.data.id, body.data.name, body.data.email, body.data.roles)
                    Result.success(body.data)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                Result.failure(Exception("Failed to get user data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update player profile.
     */
    suspend fun updateProfile(request: UpdateProfileRequest): Result<com.devwithguru.cricket.data.api.ProfileData> {
        return try {
            val token = getToken() ?: return Result.failure(Exception("Not logged in"))
            val response = apiService.updateProfile(token, request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                Result.failure(Exception("Failed to update profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logout — clear token and user data.
     */
    fun logout() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "cricket_auth_prefs"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROLES = "user_roles"
    }
}
