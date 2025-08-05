package com.team1.otvoo.feed.dto;

import java.util.List;
import java.util.UUID;

public record FeedDtoCursorResponse(
    List<FeedDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    Long totalCount,
    String sortBy,
    String sortDirection
) {

}
