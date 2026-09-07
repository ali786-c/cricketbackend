package com.devwithguru.cricket.data.mapper

import com.devwithguru.cricket.data.api.TournamentData
import com.devwithguru.cricket.data.db.entity.TournamentEntity
import com.devwithguru.cricket.domain.model.Tournament

fun TournamentData.toEntity() = TournamentEntity(
    id = id.toString(),
    serverId = id,
    name = name ?: "",
    description = description ?: "",
    logo = logo,
    coverImage = cover_image,
    organizerName = organizer_name ?: "",
    contactInfo = contact_info ?: "",
    city = city ?: "",
    venue = venue ?: "",
    season = season_name ?: "",
    startDate = starts_on ?: "",
    endDate = ends_on ?: "",
    ballType = ball_type ?: "leather",
    oversPerInnings = default_overs_per_innings ?: 20,
    wicketsPerTeam = 10, // Not explicitly provided by API in TournamentData, default to 10
    competitionStructure = competition_structure ?: "League",
    visibility = if (is_public == true) "Public" else "Private",
    tournamentCode = tournament_code ?: "",
    hasDraft = has_draft ?: false,
    squadSize = squad_size ?: 15,
    pickDuration = default_pick_duration ?: 60,
    status = status ?: "draft",
    teamCount = teams_count ?: 0,
    updatedAt = System.currentTimeMillis()
)

fun TournamentEntity.toDomain() = Tournament(
    id = id,
    serverId = serverId,
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
    wicketsPerTeam = wicketsPerTeam,
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
    serverId = id.toIntOrNull(),
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
    wicketsPerTeam = wicketsPerTeam,
    competitionStructure = competitionStructure,
    visibility = visibility,
    tournamentCode = tournamentCode,
    hasDraft = hasDraft,
    squadSize = squadSize,
    pickDuration = pickDuration,
    status = status,
    teamCount = teamCount
)
