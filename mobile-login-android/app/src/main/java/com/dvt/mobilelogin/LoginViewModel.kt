package com.dvt.mobilelogin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// One-shot events for navigation - using Channel so we don't miss events on config change
sealed interface LoginEvent {
  data object NavigateToHome : LoginEvent
}

/**
 * Handles login logic - validation, auth calls, lockout tracking.
 * Dependencies injected so we can easily test with fakes.
 */
class LoginViewModel(
  private val authRepository: AuthRepository,
  private val networkMonitor: NetworkMonitor,
  private val tokenStore: TokenStore,
) : ViewModel() {

  private val _uiState = MutableStateFlow(LoginUiState())
  val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

  private val eventsChannel = Channel<LoginEvent>(capacity = Channel.BUFFERED)
  val events = eventsChannel.receiveAsFlow()

  init {
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

    // Validation: keep it minimal for the assignment.
    if (snapshot.email.isBlank() || snapshot.password.length < 6) {
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
        val token = authRepository.login(snapshot.email, snapshot.password)

        if (snapshot.rememberMe) {
          tokenStore.saveToken(token)
        } else {
          tokenStore.clearToken()
        }

        _uiState.update { it.copy(isLoading = false, failureCount = 0, isLockedOut = false) }
        eventsChannel.trySend(LoginEvent.NavigateToHome)
      } catch (e: Throwable) {
        // Track failures for lockout - 3 strikes and you're out
        val nextFailureCount = _uiState.value.failureCount + 1
        val lockedOut = nextFailureCount >= 3

        _uiState.update {
          it.copy(
            isLoading = false,
            failureCount = nextFailureCount,
            isLockedOut = lockedOut,
            errorMessage = if (lockedOut) "Locked out after 3 failures" else "Invalid credentials",
          )
        }
      }
    }
  }

  fun resetForLogout() {
    val online = _uiState.value.isOnline
    _uiState.value = LoginUiState(isOnline = online)
  }
}
