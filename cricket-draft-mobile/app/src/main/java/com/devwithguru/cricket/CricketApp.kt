package com.devwithguru.cricket

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.devwithguru.cricket.data.sync.ConnectivityMonitor
import com.devwithguru.cricket.data.sync.SyncManager
import com.devwithguru.cricket.data.sync.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class CricketApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var connectivityMonitor: ConnectivityMonitor
    @Inject lateinit var syncManager: SyncManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Schedule background sync worker
        SyncWorker.schedule(this)

        // Auto-sync when connectivity is restored (drop first emission which is initial state)
        appScope.launch {
            connectivityMonitor.isOnline.drop(1).collect { isOnline ->
                if (isOnline) {
                    syncManager.pushPendingChanges()
                    syncManager.pullLatestData()
                }
            }
        }
    }
}
