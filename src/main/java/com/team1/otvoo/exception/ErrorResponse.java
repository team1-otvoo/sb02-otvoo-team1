package com.team1.otvoo.exception;

import java.util.Map;

public record ErrorResponse(
    String exceptionName,
    String message,
    Map<String, Object> details
) {
  public ErrorResponse(RestException e) {
    this(
        e.getErrorCode().name(),
        e.getErrorCode().getMessage(),
        e.getDetails()
    );
  }
}
