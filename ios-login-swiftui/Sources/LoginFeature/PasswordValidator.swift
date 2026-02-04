import Foundation

public enum PasswordValidationResult: Equatable {
  case valid
  case invalid([String])
}

/// Validates password strength: min 8 chars, uppercase, lowercase, number.
public struct PasswordValidator {
  public static func validate(_ password: String) -> PasswordValidationResult {
    var errors: [String] = []

    if password.count < 8 {
      errors.append("Password must be at least 8 characters")
    }

    if !password.contains(where: { $0.isUppercase }) {
      errors.append("Password must contain at least one uppercase letter")
    }

    if !password.contains(where: { $0.isLowercase }) {
      errors.append("Password must contain at least one lowercase letter")
    }

    if !password.contains(where: { $0.isNumber }) {
      errors.append("Password must contain at least one number")
    }

    return errors.isEmpty ? .valid : .invalid(errors)
  }
}
