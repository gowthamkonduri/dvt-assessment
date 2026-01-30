package com.dvt.loyalty.api;

import java.util.List;

public record ErrorResponse(
  String code,
  String message,
  List<String> details
) {
  public static ErrorResponse simple(String code, String message) {
    return new ErrorResponse(code, message, List.of());
  }
}
