package com.team1.otvoo.clothes.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import java.util.Map;

public enum SortDirection {
  ASCENDING,
  DESCENDING;

  @JsonCreator(mode = Mode.DELEGATING)
  public static SortDirection from(String value) {
    if (value == null || value.isBlank()) {
      return ASCENDING;
    }
    return switch (value.toLowerCase()) {
      case "ascending", "asc" -> ASCENDING;
      case "descending", "desc" -> DESCENDING;
      default -> throw new RestException(
          ErrorCode.INVALID_SORT_DIRECTION,
          Map.of("sortDirection", value)
      );
    };
  }
}