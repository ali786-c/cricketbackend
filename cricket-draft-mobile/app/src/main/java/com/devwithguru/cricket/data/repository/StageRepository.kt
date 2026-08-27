package com.devwithguru.cricket.data.repository

import com.devwithguru.cricket.data.db.dao.StageDao
import com.devwithguru.cricket.data.db.entity.StageEntity
import com.devwithguru.cricket.data.mapper.toDomain
import com.devwithguru.cricket.data.mapper.toEntity
import com.devwithguru.cricket.data.sync.SyncManager
import com.devwithguru.cricket.domain.model.Stage
import com.devwithguru.cricket.domain.model.StageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first repository for stage management — PRD §12-§18
 *
 * Strategy:
 * 1. WRITE to Room immediately (instant UI feedback)
 * 2. Queue change for API sync
 * 3. When online, SyncManager pushes pending changes
 */
@Singleton
class StageRepository @Inject constructor(
    private val stageDao: StageDao,
    private val syncManager: SyncManager
) {
    /**
     * Get all stages for a tournament (Flow — auto-updates on Room changes)
     */
    fun getStages(tournamentId: String): Flow<List<Stage>> =
        stageDao.getByTournament(tournamentId).map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * Get a single stage by ID
     */
    suspend fun getStageById(stageId: String): Stage? =
        stageDao.findById(stageId)?.toDomain()

    /**
     * Create a new stage — PRD §13, §15, §16
     */
    suspend fun createStage(
        tournamentId: String,
        name: String,
        type: StageType,
        order: Int,
        numberOfTeams: Int = 0,
        matchesPerTeam: Int = 0,
        pointsForWin: Int = 2,
        pointsForTie: Int = 1,
        pointsForNoResult: Int = 1,
        pointsForLoss: Int = 0,
        qualificationRule: String = "top_2",
        qualificationCount: Int = 2
    ): Stage {
        val id = "stg_${tournamentId}_${System.currentTimeMillis()}"
        val entity = StageEntity(
            id = id,
            tournamentId = tournamentId,
            name = name,
            type = type.name,
            order = order,
            numberOfTeams = numberOfTeams,
            matchesPerTeam = matchesPerTeam,
            pointsForWin = pointsForWin,
            pointsForTie = pointsForTie,
            pointsForNoResult = pointsForNoResult,
            pointsForLoss = pointsForLoss,
            qualificationRule = qualificationRule,
            qualificationCount = qualificationCount,
            status = "draft",
            syncStatus = "pending"
        )
        stageDao.insert(entity)

        syncManager.queueChange("stage", id, "create", mapOf(
            "tournamentId" to tournamentId,
            "name" to name,
            "type" to type.name,
            "order" to order.toString(),
            "qualificationRule" to qualificationRule,
            "qualificationCount" to qualificationCount.toString()
        ))

        return entity.toDomain()
    }

    /**
     * Update stage status — PRD §5 lifecycle
     */
    suspend fun updateStageStatus(stageId: String, status: String) {
        stageDao.updateStatus(stageId, status)
    }

    /**
     * Update stage team count
     */
    suspend fun updateTeamsCount(stageId: String, count: Int) {
        stageDao.updateTeamsCount(stageId, count)
    }

    /**
     * Update stage match count
     */
    suspend fun updateMatchesCount(stageId: String, count: Int) {
        stageDao.updateMatchesCount(stageId, count)
    }

    /**
     * Delete a stage
     */
    suspend fun deleteStage(stageId: String) {
        val stage = stageDao.findById(stageId) ?: return
        stageDao.deleteById(stageId)
        if (stage.serverId != null) {
            syncManager.queueChange("stage", stageId, "delete", mapOf(
                "tournamentId" to stage.tournamentId,
                "serverId" to stage.serverId.toString()
            ))
        }
    }

    /**
     * Delete all stages for a tournament
     */
    suspend fun deleteAllStages(tournamentId: String) {
        stageDao.deleteByTournament(tournamentId)
    }

    /**
     * Get stage count for a tournament
     */
    suspend fun getStageCount(tournamentId: String): Int =
        stageDao.getCountByTournament(tournamentId)
}
