package com.devwithguru.cricket.data.mapper

import com.devwithguru.cricket.data.db.entity.PlayerEntity
import com.devwithguru.cricket.domain.model.RegisteredPlayer

fun PlayerEntity.toDomain() = RegisteredPlayer(
    id = id,
    name = name,
    role = role,
    isRegistered = isRegistered,
    teamId = teamId
)

fun RegisteredPlayer.toEntity() = PlayerEntity(
    id = id,
    name = name,
    role = role,
    isRegistered = isRegistered,
    teamId = teamId
)
