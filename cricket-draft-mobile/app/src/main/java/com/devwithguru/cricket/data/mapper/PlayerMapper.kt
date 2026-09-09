package com.devwithguru.cricket.data.mapper

import com.devwithguru.cricket.data.db.entity.PlayerEntity
import com.devwithguru.cricket.domain.model.RegisteredPlayer

fun PlayerEntity.toDomain() = RegisteredPlayer(
    id = id,
    name = name,
    role = role ?: "Batter",
    isRegistered = isRegistered,
    teamId = teamId
)

fun RegisteredPlayer.toEntity() = PlayerEntity(
    id = id,
    playerProfileId = null,
    name = name,
    role = role,
    battingStyle = null,
    bowlingStyle = null,
    city = null,
    photoPath = null,
    isRegistered = isRegistered,
    teamId = teamId
)
