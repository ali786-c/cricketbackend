package com.devwithguru.cricket.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val fullName: String,
    val phone: String?,
    val city: String?,
    val playingRole: String?,
    val battingStyle: String?,
    val bowlingStyle: String?,
    val photoPath: String?,
    val bio: String?,
    val isActive: Boolean,
    val updatedAt: Long, // Epoch timestamp for conflict resolution
    // Local Sync Columns
    val localPhotoUri: String? = null,
    val syncStatus: String = "synced" // "synced", "pending_update"
)
