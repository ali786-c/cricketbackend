package com.devwithguru.cricket.domain.match

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchContractTest {
    @Test
    fun `canonical wire values match API v1`() {
        assertEquals("squad_selection", MatchLifecycle.SQUAD_SELECTION.wireValue)
        assertEquals("innings_break", MatchLifecycle.INNINGS_BREAK.wireValue)
        assertEquals("tape_ball", BallType.TAPE_BALL.wireValue)
        assertEquals("run_out", DismissalType.RUN_OUT.wireValue)
    }

    @Test
    fun `xi squad and wickets remain independent and valid`() {
        val configuration = validConfiguration().copy(squadSize = 15, playingXiSize = 11, maximumWickets = 8)
        assertTrue(configuration.validationErrors().isEmpty())
    }

    @Test
    fun `invalid cross-field configuration reports stable field keys`() {
        val errors = validConfiguration().copy(squadSize = 6, playingXiSize = 8, maximumWickets = 8).validationErrors()
        assertTrue(errors.containsKey("squad_size"))
        assertTrue(errors.containsKey("maximum_wickets"))
    }

    private fun validConfiguration() = MatchConfiguration(
        format = MatchFormat.CUSTOM, inningsPerSide = 1, oversPerInnings = 10,
        squadSize = 12, playingXiSize = 11, maximumWickets = 10,
        legalBallsPerOver = 6, maxOversPerBowler = 2, ballType = BallType.TENNIS,
        origin = ConfigurationOrigin.CUSTOM, version = 1,
    )
}
