package com.dvt.loyalty.service;

import java.util.List;

/**
 * Exception thrown when request validation fails.
 *
 * Contains a list of human-readable error messages describing each validation failure.
 * Typically mapped to HTTP 400 Bad Request with error code "VALIDATION_ERROR".
 *
 * @see QuoteService#quote
 */
public final class ValidationException extends RuntimeException {

  private final List<String> details;

  /** Creates a new ValidationException with the specified error details. */
  public ValidationException(List<String> details) {
    super("Validation failed");
    this.details = List.copyOf(details);
  }

  /** Returns the list of validation error messages. */
  public List<String> details() {
    return details;
  }
}
