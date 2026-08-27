package com.devwithguru.cricket.data.mapper

import com.devwithguru.cricket.data.db.entity.StageEntity
import com.devwithguru.cricket.domain.model.Stage
import com.devwithguru.cricket.domain.model.StageType

fun StageEntity.toDomain() = Stage(
    id = id,
    tournamentId = tournamentId,
    name = name,
    type = try { StageType.valueOf(type) } catch (_: Exception) { StageType.POINTS_TABLE },
    order = order,
    numberOfTeams = numberOfTeams,
    matchesPerTeam = matchesPerTeam,
    pointsForWin = pointsForWin,
    pointsForTie = pointsForTie,
    pointsForNoResult = pointsForNoResult,
    pointsForLoss = pointsForLoss,
    qualificationRule = qualificationRule,
    qualificationCount = qualificationCount,
    status = status,
    teamsCount = teamsCount,
    matchesCount = matchesCount,
    completedMatches = completedMatches,
    serverId = serverId,
    syncStatus = syncStatus
)

fun Stage.toEntity() = StageEntity(
    id = id,
    tournamentId = tournamentId,
    name = name,
    type = type.name,
    order = order,
    numberOfTeams = numberOfTeams,
    matchesPerTeam = matchesPerTeam,
    pointsForWin = pointsForWin,
    pointsForTie = pointsForTie,
    pointsForNoResult = pointsForNoResult,
    pointsForLoss = pointsForLoss,
    qualificationRule = qualificationRule,
    qualificationCount = qualificationCount,
    status = status,
    teamsCount = teamsCount,
    matchesCount = matchesCount,
    completedMatches = completedMatches,
    serverId = serverId,
    syncStatus = syncStatus
)
