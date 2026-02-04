package com.dvt.mobilelogin

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InMemoryNetworkMonitor(isOnline: Boolean) : NetworkMonitor {
  private val _online = MutableStateFlow(isOnline)
  override val isOnline: StateFlow<Boolean> = _online

  fun setOnline(value: Boolean) {
    _online.value = value
  }
}

class InMemoryTokenStore : TokenStore {
  private var token: String? = null

  override suspend fun saveToken(token: String) {
    this.token = token
  }

  override suspend fun clearToken() {
    token = null
  }

  override suspend fun readToken(): String? = token
}

/**
 * FOR DEBUG/TESTING ONLY - use BuildConfig.DEBUG to conditionally include.
 * Valid credentials: user@example.com / Password1
 */
class FakeAuthRepository(private val tokenStore: TokenStore) : AuthRepository {
  override suspend fun login(email: String, password: String): String {
    // Simulate network delay
    delay(500)

    if (email == "user@example.com" && password == "Password1") {
      return "token-abc"
    }
    throw AuthException("Invalid credentials")
  }
}
