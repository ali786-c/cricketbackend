package com.devwithguru.cricket.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores individual scoring deliveries for offline persistence and sync.
 *
 * Each delivery is saved to Room immediately after scoring (prevents data loss on crash),
 * then synced to the Laravel backend via POST /api/v1/matches/{matchId}/deliveries/sync
 * when the device comes back online.
 */
@Entity(
    tableName = "pending_deliveries",
    indices = [
        Index(value = ["matchId"]),
        Index(value = ["syncStatus"])
    ]
)
data class PendingDeliveryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val matchId: String,
    val inningsNumber: Int,

    // Delivery details
    val runsOffBat: Int = 0,
    val wides: Int = 0,
    val noBalls: Int = 0,
    val byes: Int = 0,
    val legByes: Int = 0,
    val penaltyRuns: Int = 0,

    // Player IDs (server-side integer IDs)
    val strikerId: Int = 0,
    val nonStrikerId: Int = 0,
    val bowlerId: Int = 0,

    // Wicket info (null if no wicket)
    val wicketDismissedPlayerId: Int? = null,
    val wicketDismissalType: String? = null,
    val wicketFielderId: Int? = null,
    val wicketRunsCompleted: Int = 0,

    // Ordering & dedup
    val overNumber: Int = 0,
    val ballNumber: Int = 0,
    val localUuid: String = java.util.UUID.randomUUID().toString(),
    val deviceTimestamp: Long = System.currentTimeMillis(),

    // Sync state
    val syncStatus: String = "pending", // "pending", "syncing", "synced", "failed"
    val retryCount: Int = 0,
    val lastError: String? = null,

    // For match state reconstruction
    val cumulativeRuns: Int = 0,
    val cumulativeWickets: Int = 0,
    val cumulativeBalls: Int = 0
)
