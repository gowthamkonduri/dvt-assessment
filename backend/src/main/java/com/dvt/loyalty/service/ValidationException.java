package com.dvt.loyalty.service;

import java.util.List;

public final class ValidationException extends RuntimeException {

  private final List<String> details;

  public ValidationException(List<String> details) {
    super("Validation failed");
    this.details = List.copyOf(details);
  }

  public List<String> details() {
    return details;
  }
}
