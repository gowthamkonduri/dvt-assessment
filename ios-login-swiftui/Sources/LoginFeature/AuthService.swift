import Foundation

public struct AuthError: Error, Equatable {
  public let message: String
  public init(_ message: String) { self.message = message }
}

public protocol AuthServicing {
  func login(email: String, password: String) async throws -> String
}

public protocol TokenStoring {
  func saveToken(_ token: String) async
  func clearToken() async
  func readToken() async -> String?
}

public final class UserDefaultsTokenStore: TokenStoring {
  private let defaults: UserDefaults
  private let key: String

  public init(defaults: UserDefaults = .standard, key: String = "LoginFeature.token") {
    self.defaults = defaults
    self.key = key
  }

  public func saveToken(_ token: String) async {
    defaults.set(token, forKey: key)
  }

  public func clearToken() async {
    defaults.removeObject(forKey: key)
  }

  public func readToken() async -> String? {
    defaults.string(forKey: key)
  }
}

/// Minimal fake auth service for manual testing.
///
/// Valid credentials: `user@example.com` / `password`
public struct FakeAuthService: AuthServicing {
  public init() {}

  public func login(email: String, password: String) async throws -> String {
    if email == "user@example.com" && password == "password" {
      return "token-abc"
    }
    throw AuthError("Invalid credentials")
  }
}
