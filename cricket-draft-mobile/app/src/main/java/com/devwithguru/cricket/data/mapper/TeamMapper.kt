package com.devwithguru.cricket.data.mapper

import com.devwithguru.cricket.data.db.entity.TeamEntity
import com.devwithguru.cricket.domain.model.Team

fun TeamEntity.toDomain() = Team(
    id = id,
    serverId = serverId,
    name = name,
    shortName = shortName,
    tournamentId = tournamentId,
    tournamentName = tournamentName,
    playerCount = playerCount,
    wins = wins,
    losses = losses,
    ties = ties,
    foundedYear = foundedYear,
    captainName = captainName,
    viceCaptainName = viceCaptainName,
    wicketkeeperName = wicketkeeperName,
    creatorId = creatorId
)

fun Team.toEntity() = TeamEntity(
    id = id,
    serverId = id.toIntOrNull(),
    name = name,
    shortName = shortName,
    tournamentId = tournamentId,
    tournamentName = tournamentName,
    playerCount = playerCount,
    wins = wins,
    losses = losses,
    ties = ties,
    foundedYear = foundedYear,
    captainName = captainName,
    viceCaptainName = viceCaptainName,
    wicketkeeperName = wicketkeeperName,
    creatorId = creatorId
)
