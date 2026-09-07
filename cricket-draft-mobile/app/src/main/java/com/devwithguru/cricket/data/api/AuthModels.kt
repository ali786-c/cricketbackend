package com.devwithguru.cricket.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

// ─── Auth Request Models ────────────────────────────────────

data class LoginRequest(
    val email: String,
    val password: String,
    val device_name: String = "android-app",
    val client_slug: String? = "cricket-app-android"
)

data class UpdateProfileRequest(
    val full_name: String,
    val phone: String? = null,
    val city: String? = null,
    val playing_role: String,
    val batting_style: String? = null,
    val bowling_style: String? = null,
    val bio: String? = null
)

// ─── Auth Response Models ───────────────────────────────────

data class LoginResponse(
    val data: UserData,
    val token: String,
    val token_type: String
)

data class UserData(
    val id: Int,
    val name: String,
    val email: String,
    val roles: List<String>,
    val permissions: List<String>,
    val player_profile: PlayerProfileData?
)

data class PlayerProfileData(
    val id: Int,
    val full_name: String?,
    val playing_role: String?
)

data class ProfileResponse(
    val data: ProfileData
)

data class ProfileData(
    val id: Int,
    val full_name: String?,
    val phone: String?,
    val city: String?,
    val playing_role: String?,
    val batting_style: String?,
    val bowling_style: String?,
    val bio: String?,
    val is_active: Boolean
)

data class MessageResponse(
    val message: String
)

data class UserResponse(
    val data: UserData
)
