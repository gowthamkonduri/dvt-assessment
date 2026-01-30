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
  val isLockedOut: Boolean = false,

  val errorMessage: String? = null,
) {
  // Button only enabled when we're ready to actually attempt login
  val isLoginEnabled: Boolean
    get() =
      !isLoading &&
        !isLockedOut &&
        isOnline &&
        email.isNotBlank() &&
        password.length >= 6
}
