package com.devwithguru.cricket.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Background worker that syncs data periodically.
 *
 * Retry policy: partial success counts as success (the next periodic run
 * finishes the rest). We only retry while progress is being made and give up
 * permanently after 3 fruitless attempts — this prevents the exponential
 * backoff loop seen when permanently-invalid changes (e.g. 422 validation
 * failures) sit in the queue.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager,
    private val connectivityMonitor: ConnectivityMonitor
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            if (!connectivityMonitor.isCurrentlyOnline()) {
                return Result.success() // Nothing to sync when offline
            }

            val result = syncManager.fullSync()
            if (result.success) {
                Result.success()
            } else {
                // "Pushed X, failed Y" — progress means at least one item got through
                val hadProgress = syncManager.syncMessage.value
                    ?.let { Regex("Pushed ([1-9]\\d*)").find(it)?.groupValues?.get(1)?.toIntOrNull() ?: 0 }
                    ?.let { it > 0 } ?: false
                if (hadProgress && runAttemptCount < MAX_CONSECUTIVE_FAILURES) {
                    Result.retry()
                } else {
                    Log.w(TAG, "Sync gave up after $runAttemptCount attempts: ${result.message}")
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Sync attempt $runAttemptCount failed", e)
            if (runAttemptCount < MAX_CONSECUTIVE_FAILURES) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val MAX_CONSECUTIVE_FAILURES = 3

        private const val WORK_NAME = "cricket_sync_work"

        /**
         * Schedule periodic background sync (every 15 minutes).
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    1, TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }

        /**
         * Cancel scheduled sync.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
