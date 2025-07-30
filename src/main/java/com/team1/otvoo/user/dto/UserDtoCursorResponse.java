package com.team1.otvoo.user.dto;

import java.util.List;
import java.util.UUID;

public record UserDtoCursorResponse(
    List<UserDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    SortDirection sortDirection
) {
  public enum SortDirection {
    ASCENDING, DESCENDING
  }
}