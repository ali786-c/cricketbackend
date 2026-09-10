package com.devwithguru.cricket.data.api

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phase 0 regression specifications for the legacy MatchStateData mapper.
 *
 * These tests intentionally describe the required behavior. They remain red until
 * the lossy ScheduledFixture mapper is replaced/fixed in Phase 10.
 */
class MatchStateMappingRegressionTest {

    @Test
    fun `innings one score is exposed as current score`() {
        val state = matchState(
            innings = listOf(
                innings(
                    number = 1,
                    battingTeam = team(2, "Away"),
                    bowlingTeam = team(1, "Home"),
                    runs = 87,
                    wickets = 3,
                    overs = "9.4",
                ),
            ),
        )

        val fixture = state.toScheduledFixture(matchId = "91")

        assertEquals(87, fixture.currentRuns)
        assertEquals(3, fixture.currentWickets)
        assertEquals("9.4", fixture.oversBowled)
        assertEquals(1, fixture.currentInnings)
    }

    @Test
    fun `batting order does not redefine fixture home and away teams`() {
        val state = matchState(
            innings = listOf(
                innings(
                    number = 1,
                    battingTeam = team(2, "Away"),
                    bowlingTeam = team(1, "Home"),
                    runs = 1,
                    wickets = 0,
                    overs = "0.1",
                ),
            ),
        )

        val fixture = state.toScheduledFixture(matchId = "91")

        // The state contract must eventually carry fixture home/away independently.
        assertEquals("Home", fixture.homeTeam)
        assertEquals("Away", fixture.awayTeam)
    }

    private fun matchState(innings: List<InningsData>) = MatchStateData(
        id = 91,
        revision = 4,
        status = "live",
        result_type = null,
        result_summary = null,
        winner_team_id = null,
        overs_per_innings = 10,
        innings = innings,
    )

    private fun innings(
        number: Int,
        battingTeam: TeamData,
        bowlingTeam: TeamData,
        runs: Int,
        wickets: Int,
        overs: String,
    ) = InningsData(
        id = number,
        number = number,
        batting_team = battingTeam,
        bowling_team = bowlingTeam,
        runs = runs,
        wickets = wickets,
        legal_balls = 0,
        maximum_overs = 10,
        overs = overs,
        target = null,
        status = "live",
        batting = emptyList(),
        bowling = emptyList(),
        recent_deliveries = emptyList(),
    )

    private fun team(id: Int, name: String) = TeamData(
        id = id,
        name = name,
        short_name = name.take(3).uppercase(),
    )
}
