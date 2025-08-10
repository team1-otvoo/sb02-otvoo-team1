package com.team1.otvoo.notification.dto;

import java.util.List;
import java.util.UUID;

public record NotificationDtoCursorResponse(
    List<NotificationDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    String sortDirection
) {
  public NotificationDtoCursorResponse(
      List<NotificationDto> data,
      String nextCursor,
      UUID nextIdAfter,
      boolean hasNext,
      long totalCount
  ) {
    this(data, nextCursor, nextIdAfter, hasNext, totalCount, "createdAt", "DESCENDING");
  }
}
