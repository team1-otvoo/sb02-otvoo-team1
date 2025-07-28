package com.team1.otvoo.exception;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;

@Getter
public class RestException extends RuntimeException {

  private final LocalDateTime timestamp;
  private final ErrorCode errorCode;
  private final Map<String, Object> details;

  public RestException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.timestamp = LocalDateTime.now();
    this.errorCode = errorCode;
    this.details = Collections.emptyMap();
  }

  public RestException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode.getMessage());
    this.timestamp = LocalDateTime.now();
    this.errorCode = errorCode;
    this.details = details;
  }
}