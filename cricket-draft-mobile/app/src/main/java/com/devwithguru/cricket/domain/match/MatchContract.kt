package com.devwithguru.cricket.domain.match

/** Canonical, platform-independent match contract shared with API v1. */
data class MatchConfiguration(
    val format: MatchFormat,
    val inningsPerSide: Int,
    val oversPerInnings: Int,
    val squadSize: Int?,
    val playingXiSize: Int,
    val maximumWickets: Int,
    val legalBallsPerOver: Int,
    val maxOversPerBowler: Int?,
    val ballType: BallType,
    val noBallRuns: Int = 1,
    val wideRuns: Int = 1,
    val wideRunsToBatsman: Boolean = false,
    val noBallRunsToBatsman: Boolean = false,
    val lastManStanding: Boolean = false,
    val maxBallsPerOver: Int? = null,
    val maxRunsPerOver: Int? = null,
    val origin: ConfigurationOrigin,
    val version: Int,
    /** ISO-8601 instant; null until the configuration becomes immutable. */
    val lockedAt: String? = null,
) {
    fun validationErrors(): Map<String, String> = buildMap {
        if (inningsPerSide !in 1..4) put("innings_per_side", "Must be between 1 and 4")
        if (oversPerInnings !in 1..100) put("overs_per_innings", "Must be between 1 and 100")
        if (playingXiSize !in 2..99) put("playing_xi_size", "Must be between 2 and 99")
        if (maximumWickets !in 1 until playingXiSize) put("maximum_wickets", "Must be below playing XI size")
        if (squadSize != null && squadSize < playingXiSize) put("squad_size", "Must be at least playing XI size")
        if (legalBallsPerOver !in 1..12) put("legal_balls_per_over", "Must be between 1 and 12")
        if (maxOversPerBowler != null && maxOversPerBowler !in 1..oversPerInnings) put("max_overs_per_bowler", "Must not exceed innings overs")
        if (noBallRuns !in 0..10) put("no_ball_runs", "Must be between 0 and 10")
        if (wideRuns !in 0..10) put("wide_runs", "Must be between 0 and 10")
        if (maxBallsPerOver != null && maxBallsPerOver !in legalBallsPerOver..24) put("max_balls_per_over", "Must be at least legal balls and at most 24")
        if (maxRunsPerOver != null && maxRunsPerOver !in 1..100) put("max_runs_per_over", "Must be between 1 and 100")
        if (version < 1) put("version", "Must be positive")
    }

    fun requireValid(): MatchConfiguration = apply {
        require(validationErrors().isEmpty()) { validationErrors().entries.joinToString { "${it.key}: ${it.value}" } }
    }
}

enum class MatchFormat(val wireValue: String) { T10("t10"), T20("t20"), ODI("odi"), TEST("test"), LIMITED_OVERS("limited_overs"), CUSTOM("custom") }
enum class BallType(val wireValue: String) { LEATHER("leather"), TENNIS("tennis"), HARD_BALL("hard_ball"), TAPE_BALL("tape_ball"), INDOOR("indoor") }
enum class ConfigurationOrigin(val wireValue: String) { CUSTOM("custom"), TOURNAMENT("tournament"), LEGACY_REVIEW("legacy_review") }
enum class TossDecision(val wireValue: String) { BAT("bat"), FIELD("field") }
enum class MatchLifecycle(val wireValue: String) {
    LOCAL_DRAFT("local_draft"), PENDING_SYNC("pending_sync"), SCHEDULED("scheduled"),
    SQUAD_SELECTION("squad_selection"), LINEUP_PENDING("lineup_pending"), TOSS_PENDING("toss_pending"),
    LIVE("live"), INNINGS_BREAK("innings_break"), COMPLETED("completed"), RESULT_PENDING("result_pending"),
    APPROVED("approved"), REJECTED("rejected"), ABANDONED("abandoned"), CANCELLED("cancelled")
}
enum class DeliveryExtra(val wireValue: String) { WIDE("wide"), NO_BALL("no_ball"), BYE("bye"), LEG_BYE("leg_bye"), PENALTY("penalty") }
enum class DismissalType(val wireValue: String) { BOWLED("bowled"), CAUGHT("caught"), LBW("lbw"), RUN_OUT("run_out"), STUMPED("stumped"), HIT_WICKET("hit_wicket"), RETIRED_OUT("retired_out"), OBSTRUCTING_FIELD("obstructing_field") }
enum class OutboxStatus(val wireValue: String) { PENDING("pending"), SYNCING("syncing"), SYNCED("synced"), FAILED("failed"), NEEDS_ATTENTION("needs_attention") }

@JvmInline value class LocalMatchId(val value: String)
@JvmInline value class ServerFixtureId(val value: Long)
@JvmInline value class ServerMatchId(val value: Long)
@JvmInline value class TournamentId(val value: Long)
@JvmInline value class TeamId(val value: Long)
@JvmInline value class PlayerId(val value: Long)
@JvmInline value class MatchPlayerId(val value: Long)
@JvmInline value class InningsId(val value: Long)

data class ApiErrorContract(
    val code: ApiErrorCode,
    val message: String,
    val fieldErrors: Map<String, List<String>> = emptyMap(),
    val retryable: Boolean,
    val currentRevision: Long? = null,
    val correlationId: String? = null,
)

enum class ApiErrorCode(val wireValue: String) {
    UNAUTHENTICATED("unauthenticated"), FORBIDDEN("forbidden"), VALIDATION_FAILED("validation_failed"),
    REVISION_CONFLICT("revision_conflict"), NOT_FOUND("not_found"), THROTTLED("throttled"),
    TRANSIENT_SERVER_ERROR("transient_server_error"), NETWORK_UNAVAILABLE("network_unavailable")
}
