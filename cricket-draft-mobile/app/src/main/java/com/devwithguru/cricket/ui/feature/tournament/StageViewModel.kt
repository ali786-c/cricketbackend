package com.devwithguru.cricket.ui.feature.tournament

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devwithguru.cricket.data.db.dao.AdminTeamDao
import com.devwithguru.cricket.data.repository.StageRepository
import com.devwithguru.cricket.domain.model.Stage
import com.devwithguru.cricket.domain.model.StageType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Stage management — PRD §12-§18
 * Handles stage creation, listing, and configuration.
 */
@HiltViewModel
class StageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val stageRepository: StageRepository,
    private val adminTeamDao: AdminTeamDao
) : ViewModel() {

    private var tournamentId: String = savedStateHandle["tournamentId"] ?: ""

    private val _stages = MutableStateFlow<List<Stage>>(emptyList())
    val stages: StateFlow<List<Stage>> = _stages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    init {
        if (tournamentId.isNotBlank()) {
            loadStages()
        }
    }

    fun setTournamentId(id: String) {
        if (this.tournamentId != id) {
            this.tournamentId = id
            loadStages()
        }
    }

    /**
     * Load all stages for this tournament from Room
     */
    fun loadStages() {
        viewModelScope.launch {
            stageRepository.getStages(tournamentId).collect {
                _stages.value = it
            }
        }
    }

    /**
     * Get the next stage number (for naming)
     */
    suspend fun getNextStageNumber(): Int =
        stageRepository.getStageCount(tournamentId) + 1

    /**
     * Get available team count for this tournament
     */
    suspend fun getTeamCount(): Int =
        adminTeamDao.getCountByTournament(tournamentId)

    /**
     * Create a new stage — PRD §13, §15-§18
     */
    fun createStage(
        name: String,
        type: StageType,
        numberOfTeams: Int,
        matchesPerTeam: Int,
        pointsWin: Int,
        pointsTie: Int,
        qualificationRule: String,
        qualificationCount: Int
    ) {
        viewModelScope.launch {
            val order = getNextStageNumber()
            stageRepository.createStage(
                tournamentId = tournamentId,
                name = name,
                type = type,
                order = order,
                numberOfTeams = numberOfTeams,
                matchesPerTeam = matchesPerTeam,
                pointsForWin = pointsWin,
                pointsForTie = pointsTie,
                qualificationRule = qualificationRule,
                qualificationCount = qualificationCount
            )
            _successMessage.value = "Stage \"$name\" created"
        }
    }

    /**
     * Delete a stage
     */
    fun deleteStage(stageId: String) {
        viewModelScope.launch {
            stageRepository.deleteStage(stageId)
            _successMessage.value = "Stage deleted"
        }
    }

    /**
     * Update stage status — PRD §5 lifecycle
     */
    fun updateStageStatus(stageId: String, status: String) {
        viewModelScope.launch {
            stageRepository.updateStageStatus(stageId, status)
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
