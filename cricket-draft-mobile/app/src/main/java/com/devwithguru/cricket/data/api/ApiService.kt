package com.devwithguru.cricket.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Retrofit API service for Cricket Draft backend.
 * Base URL is configured in NetworkModule.
 */
interface ApiService {

    // ─── Auth Endpoints ─────────────────────────────────────

    /**
     * Login with email/password.
     * POST /api/v1/auth/login
     */
    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    /**
     * Get current user profile.
     * GET /api/v1/auth/me
     */
    @GET("api/v1/auth/me")
    suspend fun getMe(
        @Header("Authorization") token: String
    ): Response<UserResponse>

    /**
     * Update player profile.
     * PATCH /api/v1/profile
     */
    @PATCH("api/v1/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Response<ProfileResponse>

    // ─── Tournament Endpoints (Public) ─────────────────────

    /**
     * List all public tournaments.
     * GET /api/v1/tournaments
     */
    @GET("api/v1/tournaments")
    suspend fun getTournaments(): Response<TournamentListResponse>

    /**
     * Get tournament details.
     * GET /api/v1/tournaments/{tournamentId}
     */
    @GET("api/v1/tournaments/{tournamentId}")
    suspend fun getTournament(
        @Path("tournamentId") tournamentId: String
    ): Response<TournamentDetailResponse>

    /**
     * Get tournament teams.
     * GET /api/v1/tournaments/{tournamentId}/teams
     */
    @GET("api/v1/tournaments/{tournamentId}/teams")
    suspend fun getTournamentTeams(
        @Path("tournamentId") tournamentId: String
    ): Response<TournamentTeamsResponse>

    /**
     * Get tournament players.
     * GET /api/v1/tournaments/{tournamentId}/players
     */
    @GET("api/v1/tournaments/{tournamentId}/players")
    suspend fun getTournamentPlayers(
        @Path("tournamentId") tournamentId: String
    ): Response<TournamentPlayersResponse>

    /**
     * Get tournament standings.
     * GET /api/v1/tournaments/{tournamentId}/standings
     */
    @GET("api/v1/tournaments/{tournamentId}/standings")
    suspend fun getTournamentStandings(
        @Path("tournamentId") tournamentId: String
    ): Response<TournamentStandingsResponse2>

    /**
     * Get tournament fixtures.
     * GET /api/v1/tournaments/{tournamentId}/fixtures
     */
    @GET("api/v1/tournaments/{tournamentId}/fixtures")
    suspend fun getTournamentFixtures(
        @Path("tournamentId") tournamentId: String
    ): Response<TournamentFixturesResponse2>

    // ─── Team Endpoints ────────────────────────────────────

    /**
     * Get team squad.
     * GET /api/v1/teams/{teamId}/squad
     */
    @GET("api/v1/teams/{teamId}/squad")
    suspend fun getTeamSquad(
        @Path("teamId") teamId: String
    ): Response<TeamSquadResponse>

    // ─── Match Endpoints ────────────────────────────────────

    /**
     * Get full match state including innings, batting, bowling, recent deliveries.
     * GET /api/v1/matches/{matchId}/state
     */
    @GET("api/v1/matches/{matchId}/state")
    suspend fun getMatchState(
        @Path("matchId") matchId: String,
        @Header("Authorization") token: String? = null
    ): Response<MatchStateResponse>

    /**
     * Get MVP data for a match.
     * GET /api/v1/matches/{matchId}/mvp
     */
    @GET("api/v1/matches/{matchId}/mvp")
    suspend fun getMatchMvp(
        @Path("matchId") matchId: String,
        @Header("Authorization") token: String? = null
    ): Response<MvpResponse>

    // ─── Player Endpoints ───────────────────────────────────

    /**
     * Get player stats.
     * GET /api/v1/players/{playerId}/stats
     */
    @GET("api/v1/players/{playerId}/stats")
    suspend fun getPlayerStats(
        @Path("playerId") playerId: String,
        @Header("Authorization") token: String? = null
    ): Response<PlayerStatsApiResponse>

    /**
     * Get player insights.
     * GET /api/v1/players/{playerId}/insights
     */
    @GET("api/v1/players/{playerId}/insights")
    suspend fun getPlayerInsights(
        @Path("playerId") playerId: String,
        @Header("Authorization") token: String? = null
    ): Response<PlayerInsightsApiResponse>

    /**
     * Get player's match history.
     * GET /api/v1/players/{playerId}/matches
     */
    @GET("api/v1/players/{playerId}/matches")
    suspend fun getPlayerMatches(
        @Path("playerId") playerId: String,
        @Header("Authorization") token: String? = null
    ): Response<PlayerMatchesResponse>

    /**
     * Get player's teams.
     * GET /api/v1/players/{playerId}/teams
     */
    @GET("api/v1/players/{playerId}/teams")
    suspend fun getPlayerTeams(
        @Path("playerId") playerId: String,
        @Header("Authorization") token: String? = null
    ): Response<PlayerTeamsResponse>

    // ─── Admin Tournament Endpoints ────────────────────────

    /**
     * Create a new tournament.
     * POST /api/v1/admin/tournaments
     */
    @POST("api/v1/admin/tournaments")
    suspend fun createTournament(
        @Header("Authorization") token: String,
        @Body request: CreateTournamentRequest
    ): Response<CreateTournamentResponse>

    /**
     * Update tournament.
     * PATCH /api/v1/admin/tournaments/{tournamentId}
     */
    @PATCH("api/v1/admin/tournaments/{tournamentId}")
    suspend fun updateTournament(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Body request: UpdateTournamentRequest
    ): Response<TournamentDetailResponse>

    /**
     * Change tournament status.
     * POST /api/v1/admin/tournaments/{tournamentId}/status
     */
    @POST("api/v1/admin/tournaments/{tournamentId}/status")
    suspend fun updateTournamentStatus(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Body request: UpdateStatusRequest
    ): Response<TournamentStatusResponse>

    // ─── Admin Team Endpoints ──────────────────────────────

    /**
     * List teams for a tournament.
     * GET /api/v1/admin/tournaments/{tournamentId}/teams
     */
    @GET("api/v1/admin/tournaments/{tournamentId}/teams")
    suspend fun getAdminTeams(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String
    ): Response<AdminTeamListResponse>

    /**
     * Create a team in a tournament.
     * POST /api/v1/admin/tournaments/{tournamentId}/teams
     */
    @POST("api/v1/admin/tournaments/{tournamentId}/teams")
    suspend fun createTeam(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Body request: CreateTeamRequest
    ): Response<AdminTeamDataResponse>

    /**
     * Delete a team.
     * DELETE /api/v1/admin/tournaments/{tournamentId}/teams/{teamId}
     */
    @retrofit2.http.DELETE("api/v1/admin/tournaments/{tournamentId}/teams/{teamId}")
    suspend fun deleteTeam(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Path("teamId") teamId: String
    ): Response<Unit>

    /**
     * Assign captain to a team.
     * POST /api/v1/admin/tournaments/{tournamentId}/teams/{teamId}/captain
     */
    @POST("api/v1/admin/tournaments/{tournamentId}/teams/{teamId}/captain")
    suspend fun assignCaptain(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Path("teamId") teamId: String,
        @Body request: AssignCaptainRequest
    ): Response<AdminTeamDataResponse>

    /**
     * Remove captain from a team.
     * DELETE /api/v1/admin/tournaments/{tournamentId}/teams/{teamId}/captain
     */
    @retrofit2.http.DELETE("api/v1/admin/tournaments/{tournamentId}/teams/{teamId}/captain")
    suspend fun removeCaptain(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Path("teamId") teamId: String
    ): Response<AdminTeamDataResponse>

    // ─── Admin Player Endpoints ────────────────────────────

    /**
     * List registered players for a tournament.
     * GET /api/v1/admin/tournaments/{tournamentId}/players
     */
    @GET("api/v1/admin/tournaments/{tournamentId}/players")
    suspend fun getAdminPlayers(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @retrofit2.http.Query("status") status: String? = null
    ): Response<AdminPlayerListResponse>

    /**
     * Approve a player registration.
     * POST /api/v1/admin/tournaments/{tournamentId}/players/{registrationId}/approve
     */
    @POST("api/v1/admin/tournaments/{tournamentId}/players/{registrationId}/approve")
    suspend fun approvePlayer(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Path("registrationId") registrationId: String
    ): Response<AdminPlayerData>

    /**
     * Reject a player registration.
     * POST /api/v1/admin/tournaments/{tournamentId}/players/{registrationId}/reject
     */
    @POST("api/v1/admin/tournaments/{tournamentId}/players/{registrationId}/reject")
    suspend fun rejectPlayer(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Path("registrationId") registrationId: String
    ): Response<AdminPlayerData>

    // ─── Player Registration Endpoints ─────────────────────

    /**
     * Check own registration status.
     * GET /api/v1/tournaments/{tournamentId}/registration
     */
    @GET("api/v1/tournaments/{tournamentId}/registration")
    suspend fun getMyRegistration(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String
    ): Response<RegistrationStatusResponse>

    /**
     * Register for a tournament.
     * POST /api/v1/tournaments/{tournamentId}/registration
     */
    @POST("api/v1/tournaments/{tournamentId}/registration")
    suspend fun registerForTournament(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String
    ): Response<RegistrationStoreResponse>

    // ─── Admin Draft Endpoints ─────────────────────────────

    /**
     * Get draft state (admin view).
     * GET /api/v1/admin/tournaments/{tournamentId}/draft/state
     */
    @GET("api/v1/admin/tournaments/{tournamentId}/draft/state")
    suspend fun getAdminDraftState(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String
    ): Response<DraftStateResponse>

    /**
     * Start the draft.
     * POST /api/v1/admin/tournaments/{tournamentId}/draft/start
     */
    @POST("api/v1/admin/tournaments/{tournamentId}/draft/start")
    suspend fun startDraft(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String
    ): Response<DraftStateResponse>

    /**
     * Admin selects a player for a pick.
     * POST /api/v1/admin/tournaments/{tournamentId}/draft/select-player
     */
    @POST("api/v1/admin/tournaments/{tournamentId}/draft/select-player")
    suspend fun adminSelectPlayer(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Body request: AdminSelectPlayerRequest
    ): Response<DraftStateResponse>

    /**
     * Pause the draft.
     * POST /api/v1/admin/tournaments/{tournamentId}/draft/pause
     */
    @POST("api/v1/admin/tournaments/{tournamentId}/draft/pause")
    suspend fun pauseDraft(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String
    ): Response<DraftStateResponse>

    /**
     * Resume the draft.
     * POST /api/v1/admin/tournaments/{tournamentId}/draft/resume
     */
    @POST("api/v1/admin/tournaments/{tournamentId}/draft/resume")
    suspend fun resumeDraft(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String
    ): Response<DraftStateResponse>

    /**
     * Extend the current pick timer.
     * POST /api/v1/admin/tournaments/{tournamentId}/draft/extend
     */
    @POST("api/v1/admin/tournaments/{tournamentId}/draft/extend")
    suspend fun extendDraftTimer(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Body request: ExtendTimerRequest
    ): Response<DraftStateResponse>

    /**
     * Skip an expired pick.
     * POST /api/v1/admin/tournaments/{tournamentId}/draft/skip
     */
    @POST("api/v1/admin/tournaments/{tournamentId}/draft/skip")
    suspend fun skipExpiredPick(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String
    ): Response<DraftStateResponse>

    /**
     * Undo the latest pick.
     * POST /api/v1/admin/tournaments/{tournamentId}/draft/undo
     */
    @POST("api/v1/admin/tournaments/{tournamentId}/draft/undo")
    suspend fun undoDraftPick(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String
    ): Response<DraftStateResponse>

    /**
     * Get draft state (captain view).
     * GET /api/v1/tournaments/{tournamentId}/draft/state
     */
    @GET("api/v1/tournaments/{tournamentId}/draft/state")
    suspend fun getCaptainDraftState(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String
    ): Response<DraftStateResponse>

    /**
     * Captain makes a pick.
     * POST /api/v1/tournaments/{tournamentId}/draft/pick
     */
    @POST("api/v1/tournaments/{tournamentId}/draft/pick")
    suspend fun captainDraftPick(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Body request: CaptainPickRequest
    ): Response<DraftStateResponse>

    // ─── Admin Fixture Endpoints ───────────────────────────

    /**
     * List fixtures for a tournament (admin).
     * GET /api/v1/admin/tournaments/{tournamentId}/fixtures
     */
    @GET("api/v1/admin/tournaments/{tournamentId}/fixtures")
    suspend fun getAdminFixtures(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String
    ): Response<TournamentFixturesResponse2>

    /**
     * Create a fixture.
     * POST /api/v1/admin/tournaments/{tournamentId}/fixtures
     */
    @POST("api/v1/admin/tournaments/{tournamentId}/fixtures")
    suspend fun createFixture(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Body request: CreateFixtureRequest
    ): Response<AdminFixtureResponse>

    /**
     * Update a fixture.
     * PUT /api/v1/admin/tournaments/{tournamentId}/fixtures/{fixtureId}
     */
    @PUT("api/v1/admin/tournaments/{tournamentId}/fixtures/{fixtureId}")
    suspend fun updateFixture(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Path("fixtureId") fixtureId: String,
        @Body request: CreateFixtureRequest
    ): Response<AdminFixtureResponse>

    /**
     * Delete a fixture.
     * DELETE /api/v1/admin/tournaments/{tournamentId}/fixtures/{fixtureId}
     */
    @DELETE("api/v1/admin/tournaments/{tournamentId}/fixtures/{fixtureId}")
    suspend fun deleteFixture(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Path("fixtureId") fixtureId: String
    ): Response<Unit>

    /**
     * Update fixture status.
     * POST /api/v1/admin/tournaments/{tournamentId}/fixtures/{fixtureId}/status
     */
    @POST("api/v1/admin/tournaments/{tournamentId}/fixtures/{fixtureId}/status")
    suspend fun updateFixtureStatus(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Path("fixtureId") fixtureId: String,
        @Body request: UpdateFixtureStatusRequest
    ): Response<AdminFixtureResponse>

    /**
     * Create match from fixture.
     * POST /api/v1/admin/tournaments/{tournamentId}/fixtures/{fixtureId}/create-match
     */
    @POST("api/v1/admin/tournaments/{tournamentId}/fixtures/{fixtureId}/create-match")
    suspend fun createMatchFromFixture(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Path("fixtureId") fixtureId: String
    ): Response<CreateMatchFromFixtureResponse>

    // ─── Admin Draft Setup ─────────────────────────────────

    /**
     * Save draft setup (rounds & picks).
     * PUT /api/v1/admin/tournaments/{tournamentId}/draft/setup
     */
    @PUT("api/v1/admin/tournaments/{tournamentId}/draft/setup")
    suspend fun saveDraftSetup(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Body request: DraftSetupRequest
    ): Response<SimpleMessageResponse>

    // ─── Admin Match Endpoints ─────────────────────────────

    /**
     * List matches for a tournament (admin).
     * GET /api/v1/admin/tournaments/{tournamentId}/matches
     */
    @GET("api/v1/admin/tournaments/{tournamentId}/matches")
    suspend fun getAdminMatches(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String
    ): Response<AdminMatchListResponse>

    /**
     * Get match details (admin).
     * GET /api/v1/admin/tournaments/{tournamentId}/matches/{matchId}
     */
    @GET("api/v1/admin/tournaments/{tournamentId}/matches/{matchId}")
    suspend fun getAdminMatch(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Path("matchId") matchId: String
    ): Response<AdminMatchDetailResponse>

    /**
     * Create a match from draft squads.
     * POST /api/v1/admin/tournaments/{tournamentId}/matches
     */
    @POST("api/v1/admin/tournaments/{tournamentId}/matches")
    suspend fun createMatch(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Body request: CreateMatchRequest
    ): Response<AdminMatchDetailResponse>

    /**
     * Update match overs.
     * PATCH /api/v1/admin/tournaments/{tournamentId}/matches/{matchId}/overs
     */
    @PATCH("api/v1/admin/tournaments/{tournamentId}/matches/{matchId}/overs")
    suspend fun updateMatchOvers(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Path("matchId") matchId: String,
        @Body request: UpdateOversRequest
    ): Response<AdminMatchDetailResponse>

    /**
     * Submit playing XI for a team.
     * POST /api/v1/admin/tournaments/{tournamentId}/matches/{matchId}/teams/{teamId}/playing-xi
     */
    @POST("api/v1/admin/tournaments/{tournamentId}/matches/{matchId}/teams/{teamId}/playing-xi")
    suspend fun submitPlayingXi(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Path("matchId") matchId: String,
        @Path("teamId") teamId: String,
        @Body request: PlayingXiRequest
    ): Response<AdminMatchDetailResponse>

    /**
     * Approve both playing XIs.
     * POST /api/v1/admin/tournaments/{tournamentId}/matches/{matchId}/approve-lineup
     */
    @POST("api/v1/admin/tournaments/{tournamentId}/matches/{matchId}/approve-lineup")
    suspend fun approveLineup(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Path("matchId") matchId: String
    ): Response<AdminMatchDetailResponse>

    /**
     * Record toss result.
     * POST /api/v1/admin/tournaments/{tournamentId}/matches/{matchId}/toss
     */
    @POST("api/v1/admin/tournaments/{tournamentId}/matches/{matchId}/toss")
    suspend fun recordToss(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @Path("matchId") matchId: String,
        @Body request: TossRequest
    ): Response<AdminMatchDetailResponse>

    // ─── Result Endpoints ──────────────────────────────────

    /**
     * Submit match result for approval.
     * POST /api/v1/admin/matches/{matchId}/result/submit
     */
    @POST("api/v1/admin/matches/{matchId}/result/submit")
    suspend fun submitMatchResult(
        @Header("Authorization") token: String,
        @Path("matchId") matchId: String
    ): Response<AdminMatchDetailResponse>

    /**
     * Approve match result.
     * POST /api/v1/admin/matches/{matchId}/result/approve
     */
    @POST("api/v1/admin/matches/{matchId}/result/approve")
    suspend fun approveMatchResult(
        @Header("Authorization") token: String,
        @Path("matchId") matchId: String
    ): Response<AdminMatchDetailResponse>

    // ─── Offline Sync Endpoint ─────────────────────────────

    /**
     * Sync offline deliveries.
     * POST /api/v1/matches/{matchId}/deliveries/sync
     */
    @POST("api/v1/matches/{matchId}/deliveries/sync")
    suspend fun syncDeliveries(
        @Header("Authorization") token: String,
        @Path("matchId") matchId: String,
        @Body request: SyncDeliveriesRequest
    ): Response<SyncDeliveriesResponse>

    // ─── Delivery Edit Endpoint ────────────────────────────

    /**
     * Edit a delivery (post-match correction).
     * PATCH /api/v1/deliveries/{deliveryId}
     */
    @PATCH("api/v1/deliveries/{deliveryId}")
    suspend fun editDelivery(
        @Header("Authorization") token: String,
        @Path("deliveryId") deliveryId: String,
        @Body request: EditDeliveryRequest
    ): Response<EditDeliveryResponse>

    // ─── Player Comparison Endpoint ────────────────────────

    /**
     * Compare two players in a tournament.
     * GET /api/v1/tournaments/{tournamentId}/players/compare?player1_id=X&player2_id=Y
     */
    @GET("api/v1/tournaments/{tournamentId}/players/compare")
    suspend fun comparePlayers(
        @Header("Authorization") token: String,
        @Path("tournamentId") tournamentId: String,
        @retrofit2.http.Query("player1_id") player1Id: String,
        @retrofit2.http.Query("player2_id") player2Id: String
    ): Response<PlayerComparisonResponse>

    // ─── Team Comparison Endpoint ──────────────────────────

    /**
     * Compare two teams (H2H).
     * GET /api/v1/teams/compare?team1_id=X&team2_id=Y
     */
    @GET("api/v1/teams/compare")
    suspend fun compareTeams(
        @retrofit2.http.Query("team1_id") team1Id: String,
        @retrofit2.http.Query("team2_id") team2Id: String
    ): Response<TeamComparisonResponse>

    // ─── Standings Simulation Endpoint ─────────────────────

    /**
     * Simulate standings possibilities.
     * GET /api/v1/tournaments/{tournamentId}/standings/simulate
     */
    @GET("api/v1/tournaments/{tournamentId}/standings/simulate")
    suspend fun simulateStandings(
        @Path("tournamentId") tournamentId: String
    ): Response<StandingsSimulationResponse>

    // ─── Auth Token Provider ───────────────────────────────

    /**
     * Provide a lazy token for dependencies that need Function0<String>.
     */
    @GET("api/v1/auth/me")
    suspend fun provideToken(
        @Header("Authorization") token: String
    ): Response<UserResponse>
}
