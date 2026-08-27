package com.devwithguru.cricket.domain.model

/**
 * Represents a registered player in the app.
 * Every player added to any team automatically gets a profile here.
 */
data class RegisteredPlayer(
    val id: String,
    val name: String,
    val role: String,
    val isRegistered: Boolean = false, // true = has an account/login, false = added manually
    val teamId: String? = null
)
