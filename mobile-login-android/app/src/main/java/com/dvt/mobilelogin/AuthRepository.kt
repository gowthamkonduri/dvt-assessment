package com.dvt.mobilelogin

/**
 * Auth repository: abstracts the network/service call.
 */
interface AuthRepository {
  /**
   * @return token string when successful
   */
  suspend fun login(email: String, password: String): String
}

class AuthException(message: String) : RuntimeException(message)
