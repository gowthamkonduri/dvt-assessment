import XCTest
@testable import LoginFeature

final class LoginViewModelTests: XCTestCase {

  func test_validation_enablesDisablesButton() async {
    let auth = SpyAuthService(result: .success("token"))
    let tokenStore = SpyTokenStore()
    let network = StaticNetworkMonitor(isOnline: true)

    let vm = await LoginViewModel(authService: auth, networkMonitor: network, tokenStore: tokenStore)

    await MainActor.run {
      XCTAssertFalse(vm.state.isLoginEnabled)
      vm.onEmailChanged("a@b.com")
      vm.onPasswordChanged("weak")
      XCTAssertFalse(vm.state.isLoginEnabled)  // too short, no uppercase/number
      vm.onPasswordChanged("Password1")
      XCTAssertTrue(vm.state.isLoginEnabled)
    }
  }

  func test_success_setsLoggedInTrue() async {
    let auth = SpyAuthService(result: .success("token-xyz"))
    let tokenStore = SpyTokenStore()
    let network = StaticNetworkMonitor(isOnline: true)

    let vm = await LoginViewModel(authService: auth, networkMonitor: network, tokenStore: tokenStore)

    await MainActor.run {
      vm.onEmailChanged("a@b.com")
      vm.onPasswordChanged("Password1")
    }

    await vm.loginTapped()

    await MainActor.run {
      XCTAssertTrue(vm.isLoggedIn)
      XCTAssertEqual(vm.state.failureCount, 0)
      XCTAssertNil(vm.state.errorMessage)
    }
  }

  func test_error_incrementsFailureCount() async {
    let auth = SpyAuthService(result: .failure(AuthError("Invalid credentials")))
    let tokenStore = SpyTokenStore()
    let network = StaticNetworkMonitor(isOnline: true)

    let vm = await LoginViewModel(authService: auth, networkMonitor: network, tokenStore: tokenStore)

    await MainActor.run {
      vm.onEmailChanged("a@b.com")
      vm.onPasswordChanged("Password1")
    }

    await vm.loginTapped()

    await MainActor.run {
      XCTAssertEqual(vm.state.failureCount, 1)
      XCTAssertFalse(vm.state.isLockedOut)
      XCTAssertEqual(vm.state.errorMessage, "Invalid credentials")
    }
  }

  func test_lockout_afterThreeFailures() async {
    let auth = SpyAuthService(result: .failure(AuthError("no")))
    let tokenStore = SpyTokenStore()
    let network = StaticNetworkMonitor(isOnline: true)

    let vm = await LoginViewModel(authService: auth, networkMonitor: network, tokenStore: tokenStore)

    await MainActor.run {
      vm.onEmailChanged("a@b.com")
      vm.onPasswordChanged("Password1")
    }

    await vm.loginTapped()
    await vm.loginTapped()
    await vm.loginTapped()

    await MainActor.run {
      XCTAssertEqual(vm.state.failureCount, 3)
      XCTAssertTrue(vm.state.isLockedOut)
      XCTAssertNotNil(vm.state.lockoutExpiry)
      XCTAssertEqual(vm.state.errorMessage, "Too many failed attempts. Try again in 5 minutes.")
    }
  }

  func test_offline_showsMessageAndDoesNotCallService() async {
    let auth = SpyAuthService(result: .success("token"))
    let tokenStore = SpyTokenStore()
    let network = StaticNetworkMonitor(isOnline: false)

    let vm = await LoginViewModel(authService: auth, networkMonitor: network, tokenStore: tokenStore)

    await MainActor.run {
      vm.onEmailChanged("a@b.com")
      vm.onPasswordChanged("Password1")
    }

    await vm.loginTapped()

    await MainActor.run {
      XCTAssertEqual(vm.state.errorMessage, "You appear to be offline")
      XCTAssertEqual(auth.loginCallCount, 0)
    }
  }

  func test_rememberMe_persistsToken() async {
    let auth = SpyAuthService(result: .success("token-xyz"))
    let tokenStore = SpyTokenStore()
    let network = StaticNetworkMonitor(isOnline: true)

    let vm = await LoginViewModel(authService: auth, networkMonitor: network, tokenStore: tokenStore)

    await MainActor.run {
      vm.onEmailChanged("a@b.com")
      vm.onPasswordChanged("Password1")
      vm.onRememberMeChanged(true)
    }

    await vm.loginTapped()

    let savedTokens = await tokenStore.getSavedTokens()
    let clearCallCount = await tokenStore.getClearCallCount()

    XCTAssertEqual(savedTokens, ["token-xyz"])
    XCTAssertEqual(clearCallCount, 0)
  }
}

// MARK: - Test doubles

private final actor SpyTokenStore: TokenStoring {
  private(set) var savedTokens: [String] = []
  private(set) var clearCallCount: Int = 0
  private var token: String? = nil

  func getSavedTokens() -> [String] { savedTokens }

  func getClearCallCount() -> Int { clearCallCount }

  func saveToken(_ token: String) async {
    savedTokens.append(token)
    self.token = token
  }

  func clearToken() async {
    clearCallCount += 1
    token = nil
  }

  func readToken() async -> String? { token }
}

private final class SpyAuthService: AuthServicing {
  enum Result {
    case success(String)
    case failure(Error)
  }

  private let result: Result
  private(set) var loginCallCount: Int = 0
  var tokenValid: Bool = true

  init(result: Result) {
    self.result = result
  }

  func login(email: String, password: String) async throws -> String {
    loginCallCount += 1
    switch result {
    case .success(let token):
      return token
    case .failure(let err):
      throw err
    }
  }

  func validateToken(_ token: String) async throws -> Bool {
    return tokenValid
  }
}

// MARK: - Point 14: Additional Tests

final class LoginViewModelAdditionalTests: XCTestCase {

  func test_emailValidation_rejectsInvalidFormats() async {
    let invalidEmails = ["", "notanemail", "missing@tld", "@nodomain.com", "spaces in@email.com"]

    for email in invalidEmails {
      let auth = SpyAuthService(result: .success("token"))
      let tokenStore = SpyTokenStore()
      let network = StaticNetworkMonitor(isOnline: true)

      let vm = await LoginViewModel(authService: auth, networkMonitor: network, tokenStore: tokenStore)

      await MainActor.run {
        vm.onEmailChanged(email)
        vm.onPasswordChanged("Password1")
        XCTAssertFalse(vm.state.isLoginEnabled, "Email '\(email)' should be invalid")
      }
    }
  }

  func test_passwordValidation_rejectsWeakPasswords() async {
    let weakPasswords = ["short", "nouppercase1", "NOLOWERCASE1", "NoNumber"]

    for password in weakPasswords {
      let auth = SpyAuthService(result: .success("token"))
      let tokenStore = SpyTokenStore()
      let network = StaticNetworkMonitor(isOnline: true)

      let vm = await LoginViewModel(authService: auth, networkMonitor: network, tokenStore: tokenStore)

      await MainActor.run {
        vm.onEmailChanged("test@example.com")
        vm.onPasswordChanged(password)
        XCTAssertFalse(vm.state.isLoginEnabled, "Password '\(password)' should be invalid")
      }
    }
  }

  func test_logout_clearsAllState() async {
    let auth = SpyAuthService(result: .success("token-xyz"))
    let tokenStore = SpyTokenStore()
    let network = StaticNetworkMonitor(isOnline: true)

    let vm = await LoginViewModel(authService: auth, networkMonitor: network, tokenStore: tokenStore)

    // Login first
    await MainActor.run {
      vm.onEmailChanged("a@b.com")
      vm.onPasswordChanged("Password1")
      vm.onRememberMeChanged(true)
    }
    await vm.loginTapped()

    await MainActor.run {
      XCTAssertTrue(vm.isLoggedIn)
    }

    // Now logout
    await vm.logoutTapped()

    let clearCallCount = await tokenStore.getClearCallCount()

    await MainActor.run {
      XCTAssertFalse(vm.isLoggedIn)
      XCTAssertEqual(vm.state.email, "")
      XCTAssertEqual(vm.state.password, "")
      XCTAssertFalse(vm.state.rememberMe)
      XCTAssertNil(vm.state.errorMessage)
    }
    XCTAssertEqual(clearCallCount, 1)
  }

  func test_rememberMe_false_doesNotPersistToken() async {
    let auth = SpyAuthService(result: .success("token-xyz"))
    let tokenStore = SpyTokenStore()
    let network = StaticNetworkMonitor(isOnline: true)

    let vm = await LoginViewModel(authService: auth, networkMonitor: network, tokenStore: tokenStore)

    await MainActor.run {
      vm.onEmailChanged("a@b.com")
      vm.onPasswordChanged("Password1")
      vm.onRememberMeChanged(false) // Remember me OFF
    }

    await vm.loginTapped()

    let savedTokens = await tokenStore.getSavedTokens()
    let clearCallCount = await tokenStore.getClearCallCount()

    XCTAssertEqual(savedTokens, [])
    XCTAssertEqual(clearCallCount, 1) // Token should be cleared
  }

  func test_tokenValidation_invalidToken_clearsAndLogsOut() async {
    let auth = SpyAuthService(result: .success("token"))
    auth.tokenValid = false
    let tokenStore = SpyTokenStore()
    await tokenStore.saveToken("old-token")
    let network = StaticNetworkMonitor(isOnline: true)

    let vm = await LoginViewModel(authService: auth, networkMonitor: network, tokenStore: tokenStore)
    await vm.checkStoredToken()

    await MainActor.run {
      XCTAssertFalse(vm.isLoggedIn)
    }
  }

  func test_tokenValidation_validToken_logsIn() async {
    let auth = SpyAuthService(result: .success("token"))
    auth.tokenValid = true
    let tokenStore = SpyTokenStore()
    await tokenStore.saveToken("valid-token")
    let network = StaticNetworkMonitor(isOnline: true)

    let vm = await LoginViewModel(authService: auth, networkMonitor: network, tokenStore: tokenStore)
    await vm.checkStoredToken()

    await MainActor.run {
      XCTAssertTrue(vm.isLoggedIn)
    }
  }

  func test_inputSanitization_trimsWhitespace() async {
    let auth = SpyAuthService(result: .success("token"))
    let tokenStore = SpyTokenStore()
    let network = StaticNetworkMonitor(isOnline: true)

    let vm = await LoginViewModel(authService: auth, networkMonitor: network, tokenStore: tokenStore)

    await MainActor.run {
      vm.onEmailChanged("  test@example.com  ")
      vm.onPasswordChanged("  Password1  ")
      // Validation should pass despite whitespace
      XCTAssertTrue(vm.state.isLoginEnabled)
    }
  }
}
