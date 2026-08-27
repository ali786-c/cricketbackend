package com.devwithguru.cricket.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import com.devwithguru.cricket.data.db.entity.AdminFixtureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdminFixtureDao {
    @Query("SELECT * FROM admin_fixtures")
    suspend fun getAllAdminFixtures(): List<AdminFixtureEntity>

    // ── Read ──
    @Query("SELECT * FROM admin_fixtures WHERE tournamentId = :tournamentId ORDER BY roundNumber, matchNumber")
    fun getByTournament(tournamentId: String): Flow<List<AdminFixtureEntity>>

    @Query("SELECT * FROM admin_fixtures WHERE tournamentId = :tournamentId ORDER BY roundNumber, matchNumber")
    suspend fun getByTournamentList(tournamentId: String): List<AdminFixtureEntity>

    @Query("SELECT * FROM admin_fixtures WHERE stageId = :stageId ORDER BY roundNumber, matchNumber")
    fun getByStage(stageId: String): Flow<List<AdminFixtureEntity>>

    @Query("SELECT * FROM admin_fixtures WHERE stageId = :stageId ORDER BY roundNumber, matchNumber")
    suspend fun getByStageList(stageId: String): List<AdminFixtureEntity>

    @Query("SELECT * FROM admin_fixtures WHERE id = :id")
    suspend fun findById(id: String): AdminFixtureEntity?

    @Query("SELECT * FROM admin_fixtures WHERE tournamentId = :tournamentId AND status = :status ORDER BY scheduledAt")
    fun getByStatus(tournamentId: String, status: String): Flow<List<AdminFixtureEntity>>

    @Query("SELECT * FROM admin_fixtures WHERE homeTeamId = :teamId OR awayTeamId = :teamId ORDER BY scheduledDate, scheduledTime")
    fun getByTeam(teamId: String): Flow<List<AdminFixtureEntity>>

    // ── Write ──
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fixture: AdminFixtureEntity)

    @Update
    suspend fun update(fixture: AdminFixtureEntity)

    @Delete
    suspend fun delete(fixture: AdminFixtureEntity)

    @Query("DELETE FROM admin_fixtures WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM admin_fixtures WHERE tournamentId = :tournamentId")
    suspend fun deleteByTournament(tournamentId: String)

    // ── Update specific fields ──
    @Query("UPDATE admin_fixtures SET scheduledDate = :date, scheduledTime = :time, venue = :venue, city = :city WHERE id = :id")
    suspend fun updateSchedule(id: String, date: String?, time: String?, venue: String?, city: String?)

    @Query("UPDATE admin_fixtures SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE admin_fixtures SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("UPDATE admin_fixtures SET serverId = :serverId, syncStatus = :status WHERE id = :id")
    suspend fun updateServerId(id: String, serverId: Int, status: String)

    // ── Stats ──
    @Query("SELECT COUNT(*) FROM admin_fixtures WHERE tournamentId = :tournamentId")
    suspend fun getCountByTournament(tournamentId: String): Int

    @Query("SELECT COUNT(*) FROM admin_fixtures WHERE stageId = :stageId")
    suspend fun getCountByStage(stageId: String): Int

    @Query("SELECT COUNT(*) FROM admin_fixtures WHERE tournamentId = :tournamentId AND status = 'completed'")
    suspend fun getCompletedCount(tournamentId: String): Int

    @Query("SELECT * FROM admin_fixtures WHERE syncStatus = 'pending'")
    suspend fun getPendingSync(): List<AdminFixtureEntity>
}
