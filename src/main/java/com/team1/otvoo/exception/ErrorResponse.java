package com.team1.otvoo.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String message,
    Map<String, Object> details,
    String exceptionType,
    String code
) {
  public ErrorResponse(RestException e) {
    this(
        LocalDateTime.now(),
        e.getErrorCode().getStatus().value(),
        e.getMessage(),
        e.getDetails(),
        e.getClass().getSimpleName(),
        e.getErrorCode().name()
    );
  }
}
