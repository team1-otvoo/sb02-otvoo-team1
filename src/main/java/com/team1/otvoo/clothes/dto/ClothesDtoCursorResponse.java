package com.team1.otvoo.clothes.dto;

import java.util.List;
import java.util.UUID;

public record ClothesDtoCursorResponse(
    List<ClothesDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    SortBy sortBy,
    SortDirection sortDirection
) {

}
