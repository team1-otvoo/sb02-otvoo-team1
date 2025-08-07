package com.team1.otvoo.clothes.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import java.util.Map;

public enum SortBy {
  NAME,
  CREATED_AT;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static SortBy from(String value) {
    if (value == null || value.isBlank()) {
      return NAME; // 기본값
    }
    return switch (value.toLowerCase()) {
      case "name" -> NAME;
      case "createdat" -> CREATED_AT;
      default -> throw new RestException(
          ErrorCode.INVALID_SORT_BY_FIELD,
          Map.of("sortBy", value)
      );
    };
  }

  @JsonValue
  public String toValue() {
    return switch (this) {
      case NAME -> "name";
      case CREATED_AT -> "createdAt";
    };
  }
}