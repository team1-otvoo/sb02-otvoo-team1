package com.team1.otvoo.follow.dto;

import java.util.List;
import java.util.UUID;

public record FollowListResponse(
    List<FollowDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    String sortDirection
) {
  public FollowListResponse(
      List<FollowDto> data,
      String nextCursor,
      UUID nextIdAfter,
      boolean hasNext,
      long totalCount
  ) {
    this(data, nextCursor, nextIdAfter, hasNext, totalCount, "createdAt", "DESCENDING");
  }
}
