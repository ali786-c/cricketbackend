package com.devwithguru.cricket.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "admin_draft_setup")
data class AdminDraftSetupEntity(
    @PrimaryKey val tournamentId: String,
    val roundsJson: String = "[]",
    val syncStatus: String = "pending"
)
