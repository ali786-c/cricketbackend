package com.devwithguru.cricket.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import com.devwithguru.cricket.data.db.entity.StageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StageDao {
    @Query("SELECT * FROM stages WHERE tournamentId = :tournamentId ORDER BY `order` ASC")
    fun getByTournament(tournamentId: String): Flow<List<StageEntity>>

    @Query("SELECT * FROM stages WHERE tournamentId = :tournamentId ORDER BY `order` ASC")
    suspend fun getByTournamentList(tournamentId: String): List<StageEntity>

    @Query("SELECT * FROM stages WHERE id = :id")
    suspend fun findById(id: String): StageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stage: StageEntity)

    @Delete
    suspend fun delete(stage: StageEntity)

    @Query("DELETE FROM stages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM stages WHERE tournamentId = :tournamentId")
    suspend fun deleteByTournament(tournamentId: String)

    @Query("UPDATE stages SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE stages SET teamsCount = :count WHERE id = :id")
    suspend fun updateTeamsCount(id: String, count: Int)

    @Query("UPDATE stages SET matchesCount = :count WHERE id = :id")
    suspend fun updateMatchesCount(id: String, count: Int)

    @Query("UPDATE stages SET completedMatches = :count WHERE id = :id")
    suspend fun updateCompletedMatches(id: String, count: Int)

    @Query("UPDATE stages SET serverId = :serverId, syncStatus = :status WHERE id = :id")
    suspend fun updateServerId(id: String, serverId: Int, status: String)

    @Query("SELECT * FROM stages WHERE syncStatus = 'pending'")
    suspend fun getPendingSync(): List<StageEntity>

    @Query("SELECT COUNT(*) FROM stages WHERE tournamentId = :tournamentId")
    suspend fun getCountByTournament(tournamentId: String): Int
}
