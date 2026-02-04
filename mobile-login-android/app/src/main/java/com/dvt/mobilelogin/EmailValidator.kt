package com.dvt.mobilelogin

/**
 * Validates email format using regex.
 */
object EmailValidator {
  private val EMAIL_REGEX = """^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$""".toRegex()

  fun isValid(email: String): Boolean {
    return email.trim().matches(EMAIL_REGEX)
  }
}
