package com.devwithguru.cricket.ui.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devwithguru.cricket.data.repository.FixtureRepository
import com.devwithguru.cricket.data.repository.TournamentRepository
import com.devwithguru.cricket.domain.model.ScheduledFixture
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerMatchesViewModel @Inject constructor(
    private val fixtureRepository: FixtureRepository,
    private val tournamentRepository: TournamentRepository
) : ViewModel() {

    private val _fixtures = MutableStateFlow<List<ScheduledFixture>>(emptyList())
    val fixtures: StateFlow<List<ScheduledFixture>> = _fixtures

    private val _tournamentContexts = MutableStateFlow<Map<String, MatchTournamentContext>>(emptyMap())
    val tournamentContexts: StateFlow<Map<String, MatchTournamentContext>> = _tournamentContexts

    fun loadAllFixtures() {
        viewModelScope.launch {
            fixtureRepository.getAllFixtures().collect {
                _fixtures.value = it.sortedByDescending(::recencyKey)
                _tournamentContexts.value = it.mapNotNull { fixture ->
                    val tournamentId = fixtureRepository.getAdminFixtureById(fixture.id)?.tournamentId
                        ?.takeIf { id -> id.isNotBlank() && id != "0" && id != "custom" }
                        ?: return@mapNotNull null
                    val name = tournamentRepository.getTournamentById(tournamentId)?.name ?: "Tournament"
                    fixture.id to MatchTournamentContext(tournamentId, name)
                }.toMap()
            }
        }
    }

    private fun recencyKey(fixture: ScheduledFixture): Long {
        fixture.id.removePrefix("m_").toLongOrNull()?.let { return it }
        val raw = "${fixture.date} ${fixture.time}".trim()
        val formats = listOf("yyyy-MM-dd HH:mm", "dd MMM yyyy HH:mm", "dd MMMM yyyy HH:mm")
        for (format in formats) {
            val parsed = runCatching {
                java.text.SimpleDateFormat(format, java.util.Locale.US).apply { isLenient = false }.parse(raw)?.time
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return 0L
    }

    fun loadFixturesByStatus(status: String) {
        viewModelScope.launch {
            fixtureRepository.getFixturesByStatus(status).collect {
                _fixtures.value = it
            }
        }
    }
}

data class MatchTournamentContext(val id: String, val name: String)
