package com.dvt.mobilelogin

import kotlinx.coroutines.flow.StateFlow

/**
 * Minimal network monitor.
 */
interface NetworkMonitor {
  val isOnline: StateFlow<Boolean>
}
