package com.devwithguru.cricket.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.devwithguru.cricket.data.db.entity.TournamentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentDao {
    @Query("SELECT * FROM tournaments ORDER BY season DESC")
    fun getAllTournaments(): Flow<List<TournamentEntity>>

    @Query("SELECT * FROM tournaments WHERE id = :id")
    suspend fun findById(id: String): TournamentEntity?

    @Query("SELECT * FROM tournaments WHERE id = :id")
    fun observeById(id: String): Flow<TournamentEntity?>

    @Query("SELECT * FROM tournaments WHERE status = :status ORDER BY season DESC")
    fun getTournamentsByStatus(status: String): Flow<List<TournamentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: TournamentEntity): Long

    @Query("SELECT * FROM tournaments WHERE serverId = :serverId LIMIT 1")
    suspend fun findByServerId(serverId: Int): TournamentEntity?

    @Query("UPDATE tournaments SET serverId = :serverId, updatedAt = :updatedAt WHERE id = :localId")
    suspend fun updateServerId(localId: String, serverId: Int, updatedAt: Long = System.currentTimeMillis())

    @Update
    suspend fun updateTournament(tournament: TournamentEntity)

    @Delete
    suspend fun deleteTournament(tournament: TournamentEntity)

    @Query("UPDATE tournaments SET teamCount = :count WHERE id = :id")
    suspend fun updateTeamCount(id: String, count: Int)
}
