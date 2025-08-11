package com.team1.otvoo.comment.dto;

import java.util.List;
import java.util.UUID;

public record CommentDtoCursorResponse(
    List<CommentDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    Long totalCount,
    String sortBy,
    String sortDirection
) {

}
