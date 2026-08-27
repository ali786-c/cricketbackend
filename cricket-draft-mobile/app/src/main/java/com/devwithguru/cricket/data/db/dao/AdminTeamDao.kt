package com.devwithguru.cricket.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import com.devwithguru.cricket.data.db.entity.AdminTeamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdminTeamDao {
    @Query("SELECT * FROM admin_teams")
    suspend fun getAllAdminTeams(): List<AdminTeamEntity>

    @Query("SELECT * FROM admin_teams WHERE tournamentId = :tournamentId ORDER BY name")
    fun getByTournament(tournamentId: String): Flow<List<AdminTeamEntity>>

    @Query("SELECT * FROM admin_teams WHERE tournamentId = :tournamentId ORDER BY name")
    suspend fun getByTournamentList(tournamentId: String): List<AdminTeamEntity>

    @Query("SELECT * FROM admin_teams WHERE id = :id")
    suspend fun findById(id: String): AdminTeamEntity?

    @Query("SELECT * FROM admin_teams WHERE teamCode = :code LIMIT 1")
    suspend fun findByCode(code: String): AdminTeamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(team: AdminTeamEntity)

    @Delete
    suspend fun delete(team: AdminTeamEntity)

    @Query("DELETE FROM admin_teams WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE admin_teams SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("UPDATE admin_teams SET serverId = :serverId, syncStatus = :status WHERE id = :id")
    suspend fun updateServerId(id: String, serverId: Int, status: String)

    @Query("UPDATE admin_teams SET captainUserId = :userId, captainUserName = :userName WHERE id = :id")
    suspend fun updateCaptain(id: String, userId: Int, userName: String)

    @Query("UPDATE admin_teams SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE admin_teams SET viceCaptainName = :name WHERE id = :id")
    suspend fun updateViceCaptain(id: String, name: String)

    @Query("UPDATE admin_teams SET managerName = :name WHERE id = :id")
    suspend fun updateManager(id: String, name: String)

    @Query("UPDATE admin_teams SET playerCount = :count WHERE id = :id")
    suspend fun updatePlayerCount(id: String, count: Int)

    @Query("SELECT * FROM admin_teams WHERE syncStatus = 'pending'")
    suspend fun getPendingSync(): List<AdminTeamEntity>

    @Query("SELECT COUNT(*) FROM admin_teams WHERE tournamentId = :tournamentId")
    suspend fun getCountByTournament(tournamentId: String): Int
}
