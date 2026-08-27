package com.devwithguru.cricket.ui.feature.tournament

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devwithguru.cricket.data.api.TournamentData
import com.devwithguru.cricket.data.api.TournamentTeamData
import com.devwithguru.cricket.data.api.TournamentStandingData
import com.devwithguru.cricket.data.api.CreateTournamentRequest
import com.devwithguru.cricket.data.api.TournamentFixtureData2
import com.devwithguru.cricket.data.repository.AuthRepository
import com.devwithguru.cricket.data.repository.TournamentAdminRepository
import com.devwithguru.cricket.data.repository.TournamentApiRepository
import com.devwithguru.cricket.data.repository.TournamentRepository
import com.devwithguru.cricket.domain.model.Tournament
import com.devwithguru.cricket.data.repository.AdminLocalRepository
import com.devwithguru.cricket.data.db.entity.AdminTeamEntity
import com.devwithguru.cricket.data.repository.FixtureRepository
import com.devwithguru.cricket.domain.model.Fixture
import com.devwithguru.cricket.data.api.TeamData
import com.devwithguru.cricket.data.repository.TeamRepository
import com.devwithguru.cricket.domain.model.Team
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TournamentViewModel @Inject constructor(
    private val tournamentRepository: TournamentRepository,
    private val tournamentApiRepository: TournamentApiRepository,
    private val adminRepository: TournamentAdminRepository,
    private val authRepository: AuthRepository,
    private val localAdminRepository: AdminLocalRepository,
    private val fixtureRepository: FixtureRepository,
    private val teamRepository: TeamRepository
) : ViewModel() {

    private val _tournaments = MutableStateFlow<List<Tournament>>(emptyList())
    val tournaments: StateFlow<List<Tournament>> = _tournaments

    private val _currentTournament = MutableStateFlow<Tournament?>(null)
    val currentTournament: StateFlow<Tournament?> = _currentTournament

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _teams = MutableStateFlow<List<TournamentTeamData>>(emptyList())
    val teams: StateFlow<List<TournamentTeamData>> = _teams

    private val _standings = MutableStateFlow<List<TournamentStandingData>>(emptyList())
    val standings: StateFlow<List<TournamentStandingData>> = _standings

    private val _fixtures = MutableStateFlow<List<TournamentFixtureData2>>(emptyList())
    val fixtures: StateFlow<List<TournamentFixtureData2>> = _fixtures

    /**
     * OFFLINE-FIRST: Load tournaments.
     * 1. Emit Room data instantly (always works, even offline)
     * 2. Try API in background → save to Room → emit fresh data
     */
    fun loadAllTournaments() {
        viewModelScope.launch {
            // Step 1: Show Room data immediately (instant, offline-safe)
            tournamentRepository.getAllTournaments().collect {
                _tournaments.value = it
            }
        }

        // Step 2: Background API refresh (optional, saves to Room)
        viewModelScope.launch {
            try {
                val result = tournamentApiRepository.getTournaments()
                result.onSuccess { apiTournaments ->
                    val domainTournaments = apiTournaments.map { it.toDomain() }
                    // Save each to Room for offline access
                    domainTournaments.forEach { tournament ->
                        tournamentRepository.saveTournament(tournament)
                    }
                    // Room collector above will auto-update _tournaments
                }
                // If API fails, Room data is already showing — no error needed
            } catch (_: Exception) {
                // Offline — Room data already showing, nothing to do
            }
        }
    }

    /**
     * OFFLINE-FIRST: Load tournament details.
     * 1. Emit Room data instantly
     * 2. Try API in background → save to Room → emit fresh data
     */
    fun loadTournament(id: String) {
        // Show Room data immediately
        viewModelScope.launch {
            val roomTournament = tournamentRepository.getTournamentById(id)
            _currentTournament.value = roomTournament
        }

        // Show local teams immediately (offline-first)
        viewModelScope.launch {
            localAdminRepository.getTeams(id).collect { localTeams ->
                val count = localTeams.size
                tournamentRepository.updateTournamentTeamCount(id, count)
                _teams.value = localTeams.map { it.toTournamentTeamData() }
            }
        }

        // Show local fixtures immediately (offline-first)
        viewModelScope.launch {
            fixtureRepository.getFixtures(id).collect { localFixtures ->
                _fixtures.value = localFixtures.map { it.toTournamentFixtureData2() }
                calculateLocalStandings(id, localFixtures)
            }
        }

        // Step 2: Background API refresh
        viewModelScope.launch {
            try {
                // Tournament details
                val tournamentResult = tournamentApiRepository.getTournament(id)
                tournamentResult.onSuccess { data ->
                    val domain = data.toDomain()
                    tournamentRepository.saveTournament(domain)
                    _currentTournament.value = domain
                }

                // Teams
                try {
                    val teamsResult = tournamentApiRepository.getTournamentTeams(id)
                    teamsResult.onSuccess { apiTeams ->
                        if (apiTeams.isNotEmpty()) {
                            _teams.value = apiTeams
                            viewModelScope.launch {
                                apiTeams.forEach { apiTeam ->
                                    teamRepository.saveTeam(Team(
                                        id = apiTeam.id.toString(),
                                        name = apiTeam.name ?: "",
                                        shortName = apiTeam.short_name ?: "",
                                        tournamentId = id,
                                        playerCount = apiTeam.squad_count ?: 0,
                                        creatorId = apiTeam.creator_id
                                    ))
                                }
                            }
                        }
                    }
                } catch (_: Exception) { }

                // Standings
                try {
                    val standingsResult = tournamentApiRepository.getTournamentStandings(id)
                    standingsResult.onSuccess { _standings.value = it }
                } catch (_: Exception) { }

                // Fixtures
                try {
                    val fixturesResult = tournamentApiRepository.getTournamentFixtures(id)
                    fixturesResult.onSuccess { apiFixtures ->
                        if (apiFixtures.isNotEmpty()) {
                            _fixtures.value = apiFixtures
                        }
                    }
                } catch (_: Exception) { }

            } catch (_: Exception) {
                // Offline — Room data already showing
            }
        }
    }

    // ─── Create Tournament via API ────────────────────────
    private val _createdTournamentId = MutableStateFlow<String?>(null)
    val createdTournamentId: StateFlow<String?> = _createdTournamentId
    private val _createError = MutableStateFlow<String?>(null)
    val createError: StateFlow<String?> = _createError

    fun createTournamentViaApi(
        name: String,
        description: String = "",
        organizerName: String = "",
        contactInfo: String = "",
        city: String,
        venue: String = "",
        season: String,
        startDate: String,
        endDate: String,
        ballType: String = "Tennis Ball",
        competitionStructure: String = "League",
        visibility: String = "public",
        hasDraft: Boolean,
        squadSize: Int = 11,
        pickDuration: Int = 60,
        overs: Int = 20
    ) {
        viewModelScope.launch {
            val token = authRepository.getRawToken()
            if (token == null) {
                _createError.value = "Not logged in"
                return@launch
            }
            val slug = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-') + "-${System.currentTimeMillis()}"
            val request = CreateTournamentRequest(
                name = name, slug = slug,
                season_name = season.ifBlank { null },
                city = city.ifBlank { null },
                venue = venue.ifBlank { null },
                starts_on = startDate.ifBlank { null },
                ends_on = endDate.ifBlank { null },
                ball_type = ballType,
                has_draft = hasDraft,
                squad_size = squadSize,
                default_pick_duration = pickDuration,
                default_overs_per_innings = overs,
                description = description.ifBlank { null },
                organizer_name = organizerName.ifBlank { null },
                contact_info = contactInfo.ifBlank { null },
                competition_structure = competitionStructure.ifBlank { null }
            )
            val result = adminRepository.createTournament(token, request)
            result.onSuccess { data ->
                _createdTournamentId.value = data.id.toString()
            }
            result.onFailure { e ->
                _createError.value = e.message ?: "Failed to create tournament"
            }
        }
    }

    fun saveTournament(tournament: Tournament) {
        viewModelScope.launch {
            tournamentRepository.saveTournament(tournament)
            _currentTournament.value = tournament
        }
    }

    fun clearError() {
        _error.value = null
        _createError.value = null
        _createdTournamentId.value = null
    }

    suspend fun generateUniqueLocalId(): String {
        return tournamentRepository.generateUniqueLocalId()
    }

    private fun calculateLocalStandings(tournamentId: String, fixturesList: List<Fixture>) {
        viewModelScope.launch {
            // Load all teams of this tournament
            localAdminRepository.getTeams(tournamentId).collect { localTeams ->
                if (localTeams.isEmpty()) return@collect

                // Initialize standings map
                val standingsMap = localTeams.associate { it.id to TournamentStandingData(
                    position = 0,
                    team = com.devwithguru.cricket.data.api.TeamData(
                        id = it.serverId ?: it.id.hashCode(),
                        name = it.name,
                        short_name = it.shortName
                    ),
                    played = 0,
                    wins = 0,
                    losses = 0,
                    ties = 0,
                    no_results = 0,
                    points = 0,
                    net_run_rate = 0.0
                ) }.toMutableMap()

                // Fetch live/completed score details for each completed fixture
                for (f in fixturesList) {
                    if (f.status.lowercase() == "completed") {
                        val scoredFixture = fixtureRepository.getScheduledFixtureById(f.id)
                        if (scoredFixture != null) {
                            val homeId = f.homeTeamId
                            val awayId = f.awayTeamId

                            val homeStanding = standingsMap[homeId]
                            val awayStanding = standingsMap[awayId]

                            if (homeStanding != null && awayStanding != null) {
                                val fRuns = scoredFixture.firstInningsRuns ?: 0
                                val sRuns = scoredFixture.currentRuns ?: 0

                                // Update matches played
                                val updatedHomePlayed = homeStanding.played + 1
                                val updatedAwayPlayed = awayStanding.played + 1

                                val (homeWin, awayWin, isTie) = when {
                                    fRuns > sRuns -> Triple(1, 0, false)
                                    sRuns > fRuns -> Triple(0, 1, false)
                                    else -> Triple(0, 0, true)
                                }

                                val updatedHomeWins = homeStanding.wins + homeWin
                                val updatedHomeLosses = homeStanding.losses + awayWin
                                val updatedHomeTies = homeStanding.ties + (if (isTie) 1 else 0)
                                val updatedHomePoints = homeStanding.points + (homeWin * 2) + (if (isTie) 1 else 0)

                                val updatedAwayWins = awayStanding.wins + awayWin
                                val updatedAwayLosses = awayStanding.losses + homeWin
                                val updatedAwayTies = awayStanding.ties + (if (isTie) 1 else 0)
                                val updatedAwayPoints = awayStanding.points + (awayWin * 2) + (if (isTie) 1 else 0)

                                val homeDiff = (fRuns - sRuns).toDouble() / 20.0
                                val awayDiff = (sRuns - fRuns).toDouble() / 20.0

                                standingsMap[homeId] = homeStanding.copy(
                                    played = updatedHomePlayed,
                                    wins = updatedHomeWins,
                                    losses = updatedHomeLosses,
                                    ties = updatedHomeTies,
                                    points = updatedHomePoints,
                                    net_run_rate = (homeStanding.net_run_rate ?: 0.0) + homeDiff
                                )

                                standingsMap[awayId] = awayStanding.copy(
                                    played = updatedAwayPlayed,
                                    wins = updatedAwayWins,
                                    losses = updatedAwayLosses,
                                    ties = updatedAwayTies,
                                    points = updatedAwayPoints,
                                    net_run_rate = (awayStanding.net_run_rate ?: 0.0) + awayDiff
                                )
                            }
                        }
                    }
                }

                // Sort standings by points desc, then net run rate desc
                val sortedList = standingsMap.values
                    .sortedWith(compareByDescending<TournamentStandingData> { it.points }.thenByDescending { it.net_run_rate })
                    .mapIndexed { index, standing ->
                        standing.copy(position = index + 1)
                    }

                // Only apply local calculation if API did not return any data (or is offline/demo mode)
                if (_standings.value.isEmpty() || sortedList.any { it.played > 0 }) {
                    _standings.value = sortedList
                }
            }
        }
    }
}

fun TournamentData.toDomain() = Tournament(
    id = id.toString(),
    name = name ?: "Unknown",
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
    ballType = ball_type ?: rule_profile?.format ?: "Tennis Ball",
    oversPerInnings = rule_profile?.overs_per_innings ?: 20,
    competitionStructure = competition_structure ?: "League",
    visibility = if (is_public == true) "public" else "private",
    tournamentCode = tournament_code,
    hasDraft = has_draft ?: false,
    squadSize = squad_size ?: 11,
    pickDuration = default_pick_duration ?: 60,
    status = status ?: "draft",
    teamCount = teams_count ?: 0
)

fun AdminTeamEntity.toTournamentTeamData() = TournamentTeamData(
    id = serverId ?: id.toIntOrNull() ?: id.hashCode(),
    name = name,
    short_name = shortName,
    logo_path = logo,
    squad_count = playerCount,
    creator_id = creatorId
)

fun Fixture.toTournamentFixtureData2() = TournamentFixtureData2(
    id = id.hashCode(),
    round_number = roundNumber,
    round_name = roundName,
    match_number = matchNumber,
    scheduled_at = scheduledDate?.let { date -> scheduledTime?.let { time -> "$date $time" } ?: date } ?: "",
    timezone = "Asia/Karachi",
    venue = venue,
    city = city,
    status = status,
    home_team = TeamData(id = homeTeamId.hashCode(), name = homeTeamName, short_name = null),
    away_team = TeamData(id = awayTeamId.hashCode(), name = awayTeamName, short_name = null),
    match_id = id.hashCode(),
    match_status = status,
    toss_winner = tossWinner,
    toss_decision = tossDecision
)

