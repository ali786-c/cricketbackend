package com.devwithguru.cricket.ui.feature.tournament

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devwithguru.cricket.data.repository.FixtureRepository
import com.devwithguru.cricket.data.repository.StageRepository
import com.devwithguru.cricket.domain.model.Fixture
import com.devwithguru.cricket.domain.model.Stage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Fixture management — PRD §21-§22
 * Handles fixture scheduling, editing, and status updates.
 */
@HiltViewModel
class FixtureViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fixtureRepository: FixtureRepository,
    private val stageRepository: StageRepository
) : ViewModel() {

    private var tournamentId: String = savedStateHandle["tournamentId"] ?: ""

    fun setTournamentId(id: String) {
        if (this.tournamentId != id) {
            this.tournamentId = id
            loadFixtures()
            loadStages()
        }
    }

    private val _fixtures = MutableStateFlow<List<Fixture>>(emptyList())
    val fixtures: StateFlow<List<Fixture>> = _fixtures

    private val _stages = MutableStateFlow<List<Stage>>(emptyList())
    val stages: StateFlow<List<Stage>> = _stages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    init {
        if (tournamentId.isNotBlank()) {
            loadFixtures()
            loadStages()
        }
    }

    fun loadFixtures() {
        viewModelScope.launch {
            fixtureRepository.getFixtures(tournamentId).collect {
                _fixtures.value = it
            }
        }
    }

    private fun loadStages() {
        viewModelScope.launch {
            stageRepository.getStages(tournamentId).collect {
                _stages.value = it
            }
        }
    }

    /**
     * Schedule a new match — PRD §21
     */
    fun scheduleMatch(
        stageId: String?,
        stageName: String?,
        homeTeamId: String,
        homeTeamName: String,
        awayTeamId: String,
        awayTeamName: String,
        date: String,
        time: String,
        venue: String,
        matchType: String
    ) {
        viewModelScope.launch {
            fixtureRepository.scheduleMatch(
                tournamentId = tournamentId,
                stageId = stageId,
                stageName = stageName,
                homeTeamId = homeTeamId,
                homeTeamName = homeTeamName,
                awayTeamId = awayTeamId,
                awayTeamName = awayTeamName,
                scheduledDate = date,
                scheduledTime = time,
                venue = venue,
                matchType = matchType
            )
            _successMessage.value = "$homeTeamName vs $awayTeamName scheduled"
        }
    }

    /**
     * Update fixture schedule — PRD §22
     */
    fun updateSchedule(fixtureId: String, date: String?, time: String?, venue: String?) {
        viewModelScope.launch {
            fixtureRepository.updateSchedule(fixtureId, date, time, venue)
            _successMessage.value = "Fixture updated"
        }
    }

    /**
     * Postpone a match — PRD §22
     */
    fun postponeMatch(fixtureId: String) {
        viewModelScope.launch {
            fixtureRepository.postponeMatch(fixtureId)
            _successMessage.value = "Match postponed"
        }
    }

    /**
     * Cancel a match — PRD §22
     */
    fun cancelMatch(fixtureId: String) {
        viewModelScope.launch {
            fixtureRepository.cancelMatch(fixtureId)
            _successMessage.value = "Match cancelled"
        }
    }

    /**
     * Delete a fixture — PRD §22
     */
    fun deleteFixture(fixtureId: String) {
        viewModelScope.launch {
            fixtureRepository.deleteFixture(fixtureId)
            _successMessage.value = "Fixture deleted"
        }
    }

    fun clearMessages() {
        _successMessage.value = null
    }
}
