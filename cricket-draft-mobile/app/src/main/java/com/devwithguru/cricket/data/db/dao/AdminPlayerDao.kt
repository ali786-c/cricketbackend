package com.devwithguru.cricket.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import com.devwithguru.cricket.data.db.entity.AdminPlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdminPlayerDao {
    @Query("SELECT * FROM admin_players WHERE tournamentId = :tournamentId ORDER BY playerName")
    fun getByTournament(tournamentId: String): Flow<List<AdminPlayerEntity>>

    @Query("SELECT * FROM admin_players WHERE id = :id")
    suspend fun findById(id: String): AdminPlayerEntity?

    @Query("SELECT * FROM admin_players WHERE id = :idOrServerId OR serverId = :serverId")
    suspend fun findByIdOrServerId(idOrServerId: String, serverId: Int): List<AdminPlayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(player: AdminPlayerEntity)

    @Delete
    suspend fun delete(player: AdminPlayerEntity)

    @Query("DELETE FROM admin_players WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE admin_players SET status = :status, syncStatus = 'pending' WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE admin_players SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("SELECT * FROM admin_players WHERE syncStatus = 'pending'")
    suspend fun getPendingSync(): List<AdminPlayerEntity>
}
