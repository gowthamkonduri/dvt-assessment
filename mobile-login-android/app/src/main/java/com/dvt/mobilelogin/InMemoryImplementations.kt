package com.dvt.mobilelogin

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
 * Minimal fake repo for manual app usage.
 * Unit tests use MockK instead.
 */
class FakeAuthRepository(private val tokenStore: TokenStore) : AuthRepository {
  override suspend fun login(email: String, password: String): String {
    if (email == "user@example.com" && password == "password") {
      return "token-abc"
    }
    throw AuthException("Invalid credentials")
  }
}
