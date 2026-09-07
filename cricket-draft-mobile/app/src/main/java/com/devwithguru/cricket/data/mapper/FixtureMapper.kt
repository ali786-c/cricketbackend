package com.devwithguru.cricket.data.mapper

import com.devwithguru.cricket.data.db.entity.AdminFixtureEntity
import com.devwithguru.cricket.domain.model.Fixture
import com.devwithguru.cricket.domain.model.ScheduledFixture
import com.google.gson.Gson

fun AdminFixtureEntity.toDomain() = Fixture(
    id = id,
    serverId = serverId,
    tournamentId = tournamentId,
    stageId = stageId,
    stageName = stageName,
    roundNumber = roundNumber,
    roundName = roundName,
    matchNumber = matchNumber,
    homeTeamId = homeTeamId,
    homeTeamName = homeTeamName,
    awayTeamId = awayTeamId,
    awayTeamName = awayTeamName,
    scheduledDate = scheduledDate,
    scheduledTime = scheduledTime,
    venue = venue,
    city = city,
    matchType = matchType,
    umpire1 = umpire1,
    umpire2 = umpire2,
    status = status,
    tossWinner = tossWinner,
    tossDecision = tossDecision,
    syncStatus = syncStatus
)

fun Fixture.toEntity() = AdminFixtureEntity(
    id = id,
    serverId = serverId ?: id.toIntOrNull(),
    tournamentId = tournamentId,
    stageId = stageId,
    stageName = stageName,
    roundNumber = roundNumber,
    roundName = roundName,
    matchNumber = matchNumber,
    homeTeamId = homeTeamId,
    homeTeamName = homeTeamName,
    awayTeamId = awayTeamId,
    awayTeamName = awayTeamName,
    scheduledDate = scheduledDate,
    scheduledTime = scheduledTime,
    scheduledAt = if (scheduledDate != null && scheduledTime != null) "$scheduledDate $scheduledTime" else null,
    venue = venue,
    city = city,
    matchType = matchType,
    umpire1 = umpire1,
    umpire2 = umpire2,
    status = status,
    tossWinner = tossWinner,
    tossDecision = tossDecision,
    syncStatus = syncStatus
)

fun com.devwithguru.cricket.data.db.entity.FixtureEntity.toScheduledDomain(gson: Gson = Gson()): ScheduledFixture {
    val listStringToken = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
    val batterStateToken = object : com.google.gson.reflect.TypeToken<List<com.devwithguru.cricket.domain.model.BatterState>>() {}.type
    val bowlerStateToken = object : com.google.gson.reflect.TypeToken<List<com.devwithguru.cricket.domain.model.BowlerState>>() {}.type
    val wicketEventToken = object : com.google.gson.reflect.TypeToken<List<com.devwithguru.cricket.domain.model.WicketEvent>>() {}.type
    val partnershipToken = object : com.google.gson.reflect.TypeToken<List<com.devwithguru.cricket.domain.model.PartnershipEvent>>() {}.type

    return ScheduledFixture(
        id = id,
        homeTeam = homeTeam,
        awayTeam = awayTeam,
        overs = overs,
        ballType = ballType,
        matchType = matchType,
        wickets = wickets,
        venue = venue,
        date = date,
        time = time,
        status = status,
        tossWinner = tossWinner,
        tossDecision = tossDecision,
        currentRuns = currentRuns,
        currentWickets = currentWickets,
        oversBowled = oversBowled,
        strikerName = strikerName,
        nonStrikerName = nonStrikerName,
        bowlerName = bowlerName,
        homeSquad = gson.fromJson(homeSquad, listStringToken) ?: emptyList(),
        awaySquad = gson.fromJson(awaySquad, listStringToken) ?: emptyList(),
        currentInnings = currentInnings,
        firstInningsRuns = firstInningsRuns,
        firstInningsWickets = firstInningsWickets,
        firstInningsBatsmen = gson.fromJson(firstInningsBatsmen, batterStateToken) ?: emptyList(),
        firstInningsBowlers = gson.fromJson(firstInningsBowlers, bowlerStateToken) ?: emptyList(),
        secondInningsBatsmen = gson.fromJson(secondInningsBatsmen, batterStateToken) ?: emptyList(),
        secondInningsBowlers = gson.fromJson(secondInningsBowlers, bowlerStateToken) ?: emptyList(),
        firstInningsFOW = gson.fromJson(firstInningsFOW, wicketEventToken) ?: emptyList(),
        firstInningsPartnerships = gson.fromJson(firstInningsPartnerships, partnershipToken) ?: emptyList(),
        secondInningsFOW = gson.fromJson(secondInningsFOW, wicketEventToken) ?: emptyList(),
        secondInningsPartnerships = gson.fromJson(secondInningsPartnerships, partnershipToken) ?: emptyList(),
        activePartnershipRuns = activePartnershipRuns,
        activePartnershipBalls = activePartnershipBalls,
        firstInningsExtras = firstInningsExtras,
        secondInningsExtras = secondInningsExtras,
        firstInningsDotBalls = firstInningsDotBalls,
        secondInningsDotBalls = secondInningsDotBalls
    )
}

fun ScheduledFixture.toEntity(gson: Gson = Gson()) = com.devwithguru.cricket.data.db.entity.FixtureEntity(
    id = id,
    homeTeam = homeTeam,
    awayTeam = awayTeam,
    overs = overs,
    ballType = ballType,
    matchType = matchType,
    wickets = wickets,
    venue = venue,
    date = date,
    time = time,
    status = status,
    tossWinner = tossWinner,
    tossDecision = tossDecision,
    currentRuns = currentRuns,
    currentWickets = currentWickets,
    oversBowled = oversBowled,
    strikerName = strikerName,
    nonStrikerName = nonStrikerName,
    bowlerName = bowlerName,
    homeSquad = gson.toJson(homeSquad),
    awaySquad = gson.toJson(awaySquad),
    currentInnings = currentInnings,
    firstInningsRuns = firstInningsRuns,
    firstInningsWickets = firstInningsWickets,
    firstInningsBatsmen = gson.toJson(firstInningsBatsmen),
    firstInningsBowlers = gson.toJson(firstInningsBowlers),
    secondInningsBatsmen = gson.toJson(secondInningsBatsmen),
    secondInningsBowlers = gson.toJson(secondInningsBowlers),
    firstInningsFOW = gson.toJson(firstInningsFOW),
    firstInningsPartnerships = gson.toJson(firstInningsPartnerships),
    secondInningsFOW = gson.toJson(secondInningsFOW),
    secondInningsPartnerships = gson.toJson(secondInningsPartnerships),
    activePartnershipRuns = activePartnershipRuns,
    activePartnershipBalls = activePartnershipBalls,
    firstInningsExtras = firstInningsExtras,
    secondInningsExtras = secondInningsExtras,
    firstInningsDotBalls = firstInningsDotBalls,
    secondInningsDotBalls = secondInningsDotBalls
)
