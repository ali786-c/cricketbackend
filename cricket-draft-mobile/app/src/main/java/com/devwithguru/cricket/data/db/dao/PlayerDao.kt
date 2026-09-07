package com.devwithguru.cricket.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devwithguru.cricket.data.db.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY id")
    fun getAllPlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun findById(id: String): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(players: List<PlayerEntity>)

    @Query("SELECT EXISTS(SELECT 1 FROM players WHERE id = :id)")
    suspend fun exists(id: String): Boolean

    @Query("SELECT COUNT(*) FROM players")
    suspend fun count(): Int

    @Query("SELECT * FROM players WHERE teamId = :teamId ORDER BY id")
    fun getPlayersByTeam(teamId: String): Flow<List<PlayerEntity>>

    @Query("UPDATE players SET teamId = :teamId WHERE id = :id")
    suspend fun updateTeamId(id: String, teamId: String?)
}
