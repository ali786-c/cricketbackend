package com.devwithguru.cricket.data.mapper

import com.devwithguru.cricket.data.db.entity.TournamentEntity
import com.devwithguru.cricket.domain.model.Tournament

fun TournamentEntity.toDomain() = Tournament(
    id = id,
    name = name,
    description = description,
    logo = logo,
    coverImage = coverImage,
    organizerName = organizerName,
    contactInfo = contactInfo,
    city = city,
    venue = venue,
    season = season,
    startDate = startDate,
    endDate = endDate,
    ballType = ballType,
    oversPerInnings = oversPerInnings,
    competitionStructure = competitionStructure,
    visibility = visibility,
    tournamentCode = tournamentCode,
    hasDraft = hasDraft,
    squadSize = squadSize,
    pickDuration = pickDuration,
    status = status,
    teamCount = teamCount
)

fun Tournament.toEntity() = TournamentEntity(
    id = id,
    name = name,
    description = description,
    logo = logo,
    coverImage = coverImage,
    organizerName = organizerName,
    contactInfo = contactInfo,
    city = city,
    venue = venue,
    season = season,
    startDate = startDate,
    endDate = endDate,
    ballType = ballType,
    oversPerInnings = oversPerInnings,
    competitionStructure = competitionStructure,
    visibility = visibility,
    tournamentCode = tournamentCode,
    hasDraft = hasDraft,
    squadSize = squadSize,
    pickDuration = pickDuration,
    status = status,
    teamCount = teamCount
)
