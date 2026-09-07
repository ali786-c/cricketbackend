package com.devwithguru.cricket.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors network connectivity and provides online/offline status.
 * Notifies listeners when connectivity is restored for auto-sync.
 */
@Singleton
class ConnectivityMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // Listeners notified when connectivity is restored (from offline → online)
    private val restoreListeners = mutableListOf<() -> Unit>()

    private var wasOffline = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val wasOfflineBefore = wasOffline
            _isOnline.value = true
            wasOffline = false
            // Trigger auto-sync if we just came back online
            if (wasOfflineBefore) {
                notifyConnectivityRestored()
            }
        }

        override fun onLost(network: Network) {
            _isOnline.value = false
            wasOffline = true
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val wasOfflineBefore = !_isOnline.value
            _isOnline.value = hasInternet
            if (hasInternet && wasOfflineBefore) {
                notifyConnectivityRestored()
            }
        }
    }

    init {
        // Check initial state
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        _isOnline.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        // Register callback
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    /**
     * Check if currently online.
     */
    fun isCurrentlyOnline(): Boolean = _isOnline.value

    /**
     * Register a listener for connectivity restoration events.
     * Used by SyncManager to auto-push pending changes when coming online.
     */
    fun onConnectivityRestored(listener: () -> Unit) {
        synchronized(restoreListeners) {
            restoreListeners.add(listener)
        }
    }

    /**
     * Remove a previously registered listener.
     */
    fun removeRestoreListener(listener: () -> Unit) {
        synchronized(restoreListeners) {
            restoreListeners.remove(listener)
        }
    }

    private fun notifyConnectivityRestored() {
        synchronized(restoreListeners) {
            restoreListeners.forEach { listener ->
                try {
                    listener()
                } catch (_: Exception) {
                    // Don't let a failing listener break others
                }
            }
        }
    }

    /**
     * Cleanup (call in Application.onTerminate if needed).
     */
    fun unregister() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
            // Already unregistered
        }
    }
}
