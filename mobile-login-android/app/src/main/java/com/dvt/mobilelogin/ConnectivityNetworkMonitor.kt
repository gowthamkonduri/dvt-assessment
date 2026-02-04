package com.dvt.mobilelogin

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

/**
 * Real network monitoring using Android ConnectivityManager.
 * 
 * Monitors network availability and emits connectivity changes as a Flow.
 * Uses NET_CAPABILITY_INTERNET to ensure actual internet access, not just
 * network connection (e.g., connected to WiFi but no internet).
 * 
 * Requires permission: android.permission.ACCESS_NETWORK_STATE
 */
class ConnectivityNetworkMonitor(context: Context) : NetworkMonitor {

  private val connectivityManager = 
    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  override val isOnline: StateFlow<Boolean> = callbackFlow {
    val callback = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        trySend(true)
      }

      override fun onLost(network: Network) {
        trySend(false)
      }

      override fun onCapabilitiesChanged(
        network: Network,
        capabilities: NetworkCapabilities
      ) {
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        trySend(hasInternet)
      }
    }

    val request = NetworkRequest.Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .build()

    connectivityManager.registerNetworkCallback(request, callback)

    // Send initial state
    val currentNetwork = connectivityManager.activeNetwork
    val hasInternet = currentNetwork?.let {
      connectivityManager.getNetworkCapabilities(it)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } ?: false
    trySend(hasInternet)

    awaitClose {
      connectivityManager.unregisterNetworkCallback(callback)
    }
  }
    .distinctUntilChanged()
    .stateIn(
      scope = CoroutineScope(Dispatchers.Default),
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = false
    )
}
