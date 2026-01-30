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

  public init(
    authService: AuthServicing,
    networkMonitor: NetworkMonitoring,
    tokenStore: TokenStoring
  ) {
    self.authService = authService
    self.networkMonitor = networkMonitor
    self.tokenStore = tokenStore

    self.state = LoginState()
    self.isLoggedIn = false
    self.state.isOnline = networkMonitor.isOnline

    // Check if we have a saved token (remember me from last session)
    Task {
      self.isLoggedIn = (await tokenStore.readToken()) != nil
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
    // refresh online snapshot
    state.isOnline = networkMonitor.isOnline

    if state.email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || state.password.count < 6 {
      state.errorMessage = "Please enter a valid email and password"
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

    do {
      let token = try await authService.login(email: state.email, password: state.password)

      if state.rememberMe {
        await tokenStore.saveToken(token)
      } else {
        await tokenStore.clearToken()
      }

      state.isLoading = false
      state.failureCount = 0
      state.isLockedOut = false
      isLoggedIn = true
    } catch {
      // Track failures - lock them out after 3 bad attempts
      state.isLoading = false
      state.failureCount += 1
      state.isLockedOut = state.failureCount >= 3
      state.errorMessage = state.isLockedOut ? "Locked out after 3 failures" : "Invalid credentials"
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
