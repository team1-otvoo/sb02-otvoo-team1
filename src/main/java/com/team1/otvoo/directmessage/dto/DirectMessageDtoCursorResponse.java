package com.team1.otvoo.directmessage.dto;

import java.util.List;
import java.util.UUID;

public record DirectMessageDtoCursorResponse(
    List<DirectMessageDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    int totalCount,
    String sortBy,
    String sortDirection
) {

}