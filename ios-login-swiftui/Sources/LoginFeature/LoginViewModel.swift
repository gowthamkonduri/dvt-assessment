import Foundation

/// Handles login logic - validation, auth calls, lockout after 3 failures.
/// Dependencies are protocol-based so we can inject fakes in tests.
@MainActor
public final class LoginViewModel: ObservableObject {
  @Published public private(set) var state: LoginState
  @Published public private(set) var isLoggedIn: Bool

  private let authService: AuthServicing
  private let networkMonitor: NetworkMonitoring
  private let tokenStore: TokenStoring
  private let analytics: AnalyticsTracking?

  public init(
    authService: AuthServicing,
    networkMonitor: NetworkMonitoring,
    tokenStore: TokenStoring,
    analytics: AnalyticsTracking? = nil
  ) {
    self.authService = authService
    self.networkMonitor = networkMonitor
    self.tokenStore = tokenStore
    self.analytics = analytics

    self.state = LoginState()
    self.isLoggedIn = false
    self.state.isOnline = networkMonitor.isOnline
  }

  /// Point 13: Validate stored token with backend on app launch.
  public func checkStoredToken() async {
    guard let token = await tokenStore.readToken() else {
      isLoggedIn = false
      return
    }

    do {
      let isValid = try await authService.validateToken(token)
      isLoggedIn = isValid
      if !isValid {
        await tokenStore.clearToken()
      }
    } catch {
      isLoggedIn = false
      await tokenStore.clearToken()
    }
  }

  public func onEmailChanged(_ value: String) {
    state.email = value
    state.errorMessage = nil
  }

  public func onPasswordChanged(_ value: String) {
    state.password = value
    state.errorMessage = nil
  }

  public func onRememberMeChanged(_ value: Bool) {
    state.rememberMe = value
  }

  public func loginTapped() async {
    // Point 12: Prevent concurrent login attempts
    guard !state.isLoading else { return }

    // Refresh online snapshot
    state.isOnline = networkMonitor.isOnline

    // Point 10: Sanitize inputs before validation
    let sanitizedEmail = state.email.trimmingCharacters(in: .whitespacesAndNewlines)
    let sanitizedPassword = state.password.trimmingCharacters(in: .whitespacesAndNewlines)

    // Check input validity first (email format, password strength)
    if !state.hasValidInput {
      state.errorMessage = "Please enter a valid email and password (min 8 chars, uppercase, lowercase, number)"
      return
    }

    if !state.isOnline {
      state.errorMessage = "You appear to be offline"
      return
    }

    if state.isLockedOut {
      state.errorMessage = "Account temporarily locked"
      return
    }

    state.isLoading = true
    state.errorMessage = nil
    analytics?.trackEvent("login_attempted", parameters: nil)

    do {
      let token = try await authService.login(email: sanitizedEmail, password: sanitizedPassword)

      if state.rememberMe {
        await tokenStore.saveToken(token)
      } else {
        await tokenStore.clearToken()
      }

      state.isLoading = false
      state.failureCount = 0
      state.lockoutExpiry = nil
      isLoggedIn = true
      analytics?.trackEvent("login_success", parameters: nil)
    } catch {
      state.isLoading = false
      state.failureCount += 1
      analytics?.trackEvent("login_failed", parameters: ["error": error.localizedDescription])

      if state.failureCount >= 3 {
        state.lockoutExpiry = Date().addingTimeInterval(300)
        state.errorMessage = "Too many failed attempts. Try again in 5 minutes."
      } else {
        // Point 7: Provide specific error from AuthError
        if let authError = error as? AuthError {
          state.errorMessage = authError.message
        } else {
          state.errorMessage = "Login failed. Please try again."
        }
      }
    }
  }

  public func logoutTapped() async {
    await tokenStore.clearToken()
    isLoggedIn = false

    // Keep online state; reset everything else.
    let online = state.isOnline
    state = LoginState()
    state.isOnline = online
  }
}
