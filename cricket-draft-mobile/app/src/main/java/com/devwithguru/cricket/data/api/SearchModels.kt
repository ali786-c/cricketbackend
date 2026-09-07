package com.devwithguru.cricket.data.api

/**
 * DTOs for the unified server search endpoint (GET /api/v1/search).
 *
 * Response shape (Laravel):
 * {
 *   "data": {
 *     "players": [...],
 *     "teams": [...],
 *     "tournaments": [...],
 *     "matches": [...],
 *     "meta": { "query": "...", "is_code_search": false, "types_searched": ["players"] }
 *   }
 * }
 *
 * When a `type` filter is sent, the backend only includes the matching sections,
 * so every list must default to empty.
 */
data class SearchResponse(
    val data: SearchData? = null
)

data class SearchData(
    val players: List<SearchPlayerData> = emptyList(),
    val teams: List<SearchTeamData> = emptyList(),
    val tournaments: List<SearchTournamentData> = emptyList(),
    val matches: List<SearchMatchData> = emptyList(),
    val meta: SearchMeta? = null
)

data class SearchPlayerData(
    val id: Int,
    val unique_code: String? = null,
    val full_name: String? = null,
    val playing_role: String? = null,
    val batting_style: String? = null,
    val bowling_style: String? = null,
    val city: String? = null,
    val photo_path: String? = null
)

data class SearchTeamData(
    val id: Int,
    val unique_code: String? = null,
    val name: String? = null,
    val short_name: String? = null,
    val logo_path: String? = null,
    val tournament_id: Int? = null,
    val is_active: Boolean? = null
)

data class SearchTournamentData(
    val id: Int,
    val name: String? = null,
    val slug: String? = null,
    val status: String? = null,
    val city: String? = null,
    val starts_on: String? = null,
    val ends_on: String? = null,
    val logo_path: String? = null
)

data class SearchMatchData(
    val id: Int,
    val status: String? = null,
    val home_team: String? = null,
    val away_team: String? = null,
    val tournament: String? = null,
    val tournament_id: Int? = null
)

data class SearchMeta(
    val query: String? = null,
    val is_code_search: Boolean? = null,
    val types_searched: List<String> = emptyList()
)
