import Foundation

public struct AuthError: Error, Equatable {
  public let message: String
  public init(_ message: String) { self.message = message }
}

public protocol AuthServicing {
  func login(email: String, password: String) async throws -> String
  func validateToken(_ token: String) async throws -> Bool
}

/// Analytics tracking protocol for monitoring login events and user behavior.
public protocol AnalyticsTracking {
  func trackEvent(_ name: String, parameters: [String: Any]?)
}

public protocol TokenStoring {
  func saveToken(_ token: String) async
  func clearToken() async
  func readToken() async -> String?
}

public actor UserDefaultsTokenStore: TokenStoring {
  private let defaults: UserDefaults
  private let key: String

  public init(defaults: UserDefaults = .standard, key: String = "LoginFeature.token") {
    self.defaults = defaults
    self.key = key
  }

  public func saveToken(_ token: String) async {
    defaults.set(token, forKey: key)
    defaults.synchronize()
  }

  public func clearToken() async {
    defaults.removeObject(forKey: key)
    defaults.synchronize()
  }

  public func readToken() async -> String? {
    defaults.string(forKey: key)
  }
}

#if DEBUG
/// FOR DEMO/TESTING ONLY - excluded from release builds.
/// Valid credentials: `user@example.com` / `Password1`
public struct FakeAuthService: AuthServicing {
  public init() {}

  public func login(email: String, password: String) async throws -> String {
    // Simulate network delay
    try await Task.sleep(nanoseconds: 500_000_000)

    if email == "user@example.com" && password == "Password1" {
      return "token-abc"
    }
    throw AuthError("Invalid credentials")
  }

  public func validateToken(_ token: String) async throws -> Bool {
    try await Task.sleep(nanoseconds: 200_000_000)
    return token == "token-abc"
  }
}
#endif
