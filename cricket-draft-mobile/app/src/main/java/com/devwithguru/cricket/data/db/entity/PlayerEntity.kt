package com.devwithguru.cricket.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val role: String,
    val isRegistered: Boolean = false,
    val teamId: String? = null
)
