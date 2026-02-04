package com.dvt.mobilelogin

/**
 * UI state that the Compose screen observes.
 *
 * Keep it simple: all values needed to render are here.
 */
data class LoginUiState(
  val email: String = "",
  val password: String = "",
  val rememberMe: Boolean = false,

  val isOnline: Boolean = true,
  val isLoading: Boolean = false,

  val failureCount: Int = 0,
  val lockoutExpiryTime: Long? = null, // timestamp in millis for time-based lockout

  val errorMessage: String? = null,
) {
  // Lockout expires automatically after the time passes
  val isLockedOut: Boolean
    get() = lockoutExpiryTime?.let { it > System.currentTimeMillis() } ?: false

  // Button only enabled when we're ready to actually attempt login
  val isLoginEnabled: Boolean
    get() =
      !isLoading &&
        !isLockedOut &&
        isOnline &&
        EmailValidator.isValid(email) &&
        password.length >= 6
}
