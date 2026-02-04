package com.dvt.mobilelogin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// One-shot events for navigation - using Channel so we don't miss events on config change
sealed interface LoginEvent {
  data object NavigateToHome : LoginEvent
}

/**
 * Handles login logic - validation, auth calls, lockout tracking.
 * Dependencies injected via Hilt for testability.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
  private val authRepository: AuthRepository,
  private val networkMonitor: NetworkMonitor,
  private val tokenStore: TokenStore,
) : ViewModel() {

  private val _uiState = MutableStateFlow(LoginUiState())
  val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

  private val _isLoggedIn = MutableStateFlow(false)
  val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

  private val eventsChannel = Channel<LoginEvent>(capacity = Channel.BUFFERED)
  val events = eventsChannel.receiveAsFlow()

  init {
    // Check for existing token on startup
    viewModelScope.launch {
      _isLoggedIn.value = tokenStore.readToken() != null
    }

    // Keep UI state in sync with network status
    viewModelScope.launch {
      networkMonitor.isOnline.collect { online ->
        _uiState.update { it.copy(isOnline = online) }
      }
    }
  }

  fun onEmailChanged(value: String) {
    _uiState.update { it.copy(email = value, errorMessage = null) }
  }

  fun onPasswordChanged(value: String) {
    _uiState.update { it.copy(password = value, errorMessage = null) }
  }

  fun onRememberMeChanged(value: Boolean) {
    _uiState.update { it.copy(rememberMe = value) }
  }

  fun onLoginClicked() {
    val snapshot = _uiState.value

    // Sanitize inputs
    val sanitizedEmail = snapshot.email.trim()
    val sanitizedPassword = snapshot.password.trim()

    // Validation with proper email format check
    if (!EmailValidator.isValid(sanitizedEmail) || sanitizedPassword.length < 6) {
      _uiState.update { it.copy(errorMessage = "Please enter a valid email and password") }
      return
    }

    if (!snapshot.isOnline) {
      _uiState.update { it.copy(errorMessage = "You appear to be offline") }
      return
    }

    if (snapshot.isLockedOut) {
      _uiState.update { it.copy(errorMessage = "Account temporarily locked") }
      return
    }

    _uiState.update { it.copy(isLoading = true, errorMessage = null) }

    viewModelScope.launch {
      try {
        val token = authRepository.login(sanitizedEmail, sanitizedPassword)

        if (snapshot.rememberMe) {
          tokenStore.saveToken(token)
        } else {
          tokenStore.clearToken()
        }

        _uiState.update { it.copy(isLoading = false, failureCount = 0, lockoutExpiryTime = null) }
        _isLoggedIn.value = true
        eventsChannel.trySend(LoginEvent.NavigateToHome)
      } catch (e: Throwable) {
        // Track failures for lockout - 3 strikes with 5-minute timeout
        val nextFailureCount = _uiState.value.failureCount + 1
        val lockoutExpiry = if (nextFailureCount >= 3) {
          System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes
        } else null

        // Provide specific error messages based on exception type
        val errorMessage = when {
          lockoutExpiry != null -> "Too many failed attempts. Try again in 5 minutes."
          e is AuthException -> e.message ?: "Invalid credentials"
          e is java.net.UnknownHostException -> "Unable to reach server. Please check your connection."
          e is java.net.SocketTimeoutException -> "Request timed out. Please try again."
          e is java.io.IOException -> "Network error. Please check your connection."
          else -> "Login failed. Please try again."
        }

        _uiState.update {
          it.copy(
            isLoading = false,
            failureCount = nextFailureCount,
            lockoutExpiryTime = lockoutExpiry,
            errorMessage = errorMessage,
          )
        }
      }
    }
  }

  /**
   * Handles logout: clears token and resets UI state.
   */
  fun logout() {
    viewModelScope.launch {
      tokenStore.clearToken()
      _isLoggedIn.value = false
      val online = _uiState.value.isOnline
      _uiState.value = LoginUiState(isOnline = online)
    }
  }

  /**
   * Resets login form state (for testing or manual reset).
   */
  fun resetForLogout() {
    val online = _uiState.value.isOnline
    _uiState.value = LoginUiState(isOnline = online)
  }
}
