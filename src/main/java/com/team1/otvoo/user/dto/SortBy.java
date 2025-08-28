package com.team1.otvoo.user.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum SortBy {
  EMAIL, CREATED_AT;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static SortBy from(String value) {
    if (value == null) {
      log.warn("sortBy 값이 null 입니다.");
      throw new RestException(ErrorCode.INVALID_SORT_BY_FIELD, Map.of("sortBy", "null"));
    }

    return switch (value.toLowerCase()) {
      case "email" -> EMAIL;
      case "created_at" -> CREATED_AT;
      default -> throw new RestException(ErrorCode.INVALID_SORT_BY_FIELD, Map.of("sortBy", value));
    };
  }

  @JsonValue
  public String toValue() {
    return name().toLowerCase();
  }
}