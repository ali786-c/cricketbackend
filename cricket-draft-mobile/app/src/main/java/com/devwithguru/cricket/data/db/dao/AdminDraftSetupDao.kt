package com.devwithguru.cricket.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devwithguru.cricket.data.db.entity.AdminDraftSetupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdminDraftSetupDao {
    @Query("SELECT * FROM admin_draft_setup WHERE tournamentId = :tournamentId")
    fun getByTournament(tournamentId: String): Flow<AdminDraftSetupEntity?>

    @Query("SELECT * FROM admin_draft_setup WHERE tournamentId = :tournamentId")
    suspend fun getByTournamentOnce(tournamentId: String): AdminDraftSetupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setup: AdminDraftSetupEntity)

    @Query("UPDATE admin_draft_setup SET syncStatus = :status WHERE tournamentId = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}
