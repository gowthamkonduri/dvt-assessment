import Foundation

public struct LoginState: Equatable {
  public var email: String = ""
  public var password: String = ""
  public var rememberMe: Bool = false

  public var isOnline: Bool = true
  public var isLoading: Bool = false

  public var failureCount: Int = 0
  public var lockoutExpiry: Date?

  /// True if currently locked out (lockout hasn't expired yet).
  public var isLockedOut: Bool {
    guard let expiry = lockoutExpiry else { return false }
    return Date() < expiry
  }

  public var errorMessage: String? = nil

  public init() {}

  /// Validates email format using regex.
  public var isValidEmail: Bool {
    let trimmed = email.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !trimmed.isEmpty else { return false }
    let emailRegex = #"^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$"#
    let predicate = NSPredicate(format: "SELF MATCHES[c] %@", emailRegex)
    return predicate.evaluate(with: trimmed)
  }

  /// True if password meets strength requirements.
  public var isValidPassword: Bool {
    PasswordValidator.validate(password) == .valid
  }

  /// True if email and password inputs are valid (ignores network/loading/lockout).
  public var hasValidInput: Bool {
    isValidEmail && isValidPassword
  }

  public var isLoginEnabled: Bool {
    guard !isLoading, isOnline, !isLockedOut else { return false }
    return isValidEmail && isValidPassword
  }
}
