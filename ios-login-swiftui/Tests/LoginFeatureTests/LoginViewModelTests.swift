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
      vm.onPasswordChanged("12345")
      XCTAssertFalse(vm.state.isLoginEnabled)
      vm.onPasswordChanged("123456")
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
      vm.onPasswordChanged("123456")
    }

    await vm.loginTapped()

    await MainActor.run {
      XCTAssertTrue(vm.isLoggedIn)
      XCTAssertEqual(vm.state.failureCount, 0)
      XCTAssertNil(vm.state.errorMessage)
    }
  }

  func test_error_incrementsFailureCount() async {
    let auth = SpyAuthService(result: .failure(AuthError("no")))
    let tokenStore = SpyTokenStore()
    let network = StaticNetworkMonitor(isOnline: true)

    let vm = await LoginViewModel(authService: auth, networkMonitor: network, tokenStore: tokenStore)

    await MainActor.run {
      vm.onEmailChanged("a@b.com")
      vm.onPasswordChanged("123456")
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
      vm.onPasswordChanged("123456")
    }

    await vm.loginTapped()
    await vm.loginTapped()
    await vm.loginTapped()

    await MainActor.run {
      XCTAssertEqual(vm.state.failureCount, 3)
      XCTAssertTrue(vm.state.isLockedOut)
      XCTAssertEqual(vm.state.errorMessage, "Locked out after 3 failures")
    }
  }

  func test_offline_showsMessageAndDoesNotCallService() async {
    let auth = SpyAuthService(result: .success("token"))
    let tokenStore = SpyTokenStore()
    let network = StaticNetworkMonitor(isOnline: false)

    let vm = await LoginViewModel(authService: auth, networkMonitor: network, tokenStore: tokenStore)

    await MainActor.run {
      vm.onEmailChanged("a@b.com")
      vm.onPasswordChanged("123456")
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
      vm.onPasswordChanged("123456")
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
}
