package com.devwithguru.cricket.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.devwithguru.cricket.data.api.ApiService
import com.devwithguru.cricket.data.api.LoginRequest
import com.devwithguru.cricket.data.api.LoginResponse
import com.devwithguru.cricket.data.api.UpdateProfileRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles authentication: login, logout, token storage, profile updates.
 *
 * Uses EncryptedSharedPreferences for secure token storage (AES-256 encryption).
 * Falls back to regular SharedPreferences if encryption setup fails (e.g., no hardware keystore).
 */
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import com.devwithguru.cricket.data.db.dao.UserProfileDao
import com.devwithguru.cricket.data.db.entity.UserProfileEntity

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val userProfileDao: UserProfileDao,
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        createEncryptedPrefs()
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular SharedPreferences if encryption fails
            // (e.g., on devices without hardware keystore)
            context.getSharedPreferences("${PREFS_NAME}_fallback", Context.MODE_PRIVATE)
        }
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

    fun saveUserData(id: Int, name: String, email: String, roles: List<String>, playerProfileId: Int?) {
        val editor = prefs.edit()
            .putInt(KEY_USER_ID, id)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .putStringSet(KEY_USER_ROLES, roles.toSet())
            
        if (playerProfileId != null) {
            editor.putInt(KEY_PLAYER_PROFILE_ID, playerProfileId)
        } else {
            editor.remove(KEY_PLAYER_PROFILE_ID)
        }
        editor.apply()
    }

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)
    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""
    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""
    fun getUserRoles(): Set<String> = prefs.getStringSet(KEY_USER_ROLES, emptySet()) ?: emptySet()
    fun getPlayerProfileId(): Int = prefs.getInt(KEY_PLAYER_PROFILE_ID, -1)

    // ─── API Calls ─────────────────────────────────────────

    /**
     * Register with name, email, password. Returns LoginResponse on success.
     */
    suspend fun register(name: String, email: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.register(
                com.devwithguru.cricket.data.api.RegisterRequest(name = name, email = email, password = password)
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // Save token
                    saveToken(body.token)
                    // Save user data
                    saveUserData(body.data.id, body.data.name, body.data.email, body.data.roles, body.data.player_profile?.id)
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty response from server"))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
                    saveUserData(body.data.id, body.data.name, body.data.email, body.data.roles, body.data.player_profile?.id)
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
                    saveUserData(body.data.id, body.data.name, body.data.email, body.data.roles, body.data.player_profile?.id)
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
    /**
     * Update player profile (Multipart).
     */
    suspend fun updateProfile(request: UpdateProfileRequest): Result<com.devwithguru.cricket.data.api.ProfileData> {
        return try {
            val token = getToken() ?: return Result.failure(Exception("Not logged in"))
            
            // Convert strings to RequestBody
            val fullName = request.full_name.toRequestBody("text/plain".toMediaTypeOrNull())
            val playingRole = request.playing_role?.toRequestBody("text/plain".toMediaTypeOrNull())
            val battingStyle = request.batting_style?.toRequestBody("text/plain".toMediaTypeOrNull())
            val bowlingStyle = request.bowling_style?.toRequestBody("text/plain".toMediaTypeOrNull())
            val city = request.city?.toRequestBody("text/plain".toMediaTypeOrNull())
            val bio = request.bio?.toRequestBody("text/plain".toMediaTypeOrNull())
            // Photo is skipped for now in this wrapper, can be passed if needed
            
            val response = apiService.updateProfile(
                token = token,
                fullName = fullName,
                playingRole = playingRole,
                battingStyle = battingStyle,
                bowlingStyle = bowlingStyle,
                city = city,
                bio = bio,
                photo = null
            )
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // Update Room DB
                    userProfileDao.insertOrUpdate(
                        UserProfileEntity(
                            id = body.data.id,
                            userId = body.data.user_id,
                            fullName = body.data.full_name ?: "",
                            phone = body.data.phone,
                            city = body.data.city,
                            playingRole = body.data.playing_role,
                            battingStyle = body.data.batting_style,
                            bowlingStyle = body.data.bowling_style,
                            photoPath = body.data.photo_path,
                            bio = body.data.bio,
                            isActive = body.data.is_active,
                            updatedAt = System.currentTimeMillis() // Simple epoch
                        )
                    )
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
        private const val KEY_PLAYER_PROFILE_ID = "player_profile_id"
    }
}
