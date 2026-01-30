import Foundation

public struct LoginState: Equatable {
  public var email: String = ""
  public var password: String = ""
  public var rememberMe: Bool = false

  public var isOnline: Bool = true
  public var isLoading: Bool = false

  public var failureCount: Int = 0
  public var isLockedOut: Bool = false

  public var errorMessage: String? = nil

  public init() {}

  public var isLoginEnabled: Bool {
    guard !isLoading, isOnline, !isLockedOut else { return false }
    guard !email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return false }
    return password.count >= 6
  }
}
