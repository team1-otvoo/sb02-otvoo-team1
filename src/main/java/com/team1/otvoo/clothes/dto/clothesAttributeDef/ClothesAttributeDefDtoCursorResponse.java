package com.team1.otvoo.clothes.dto.clothesAttributeDef;

import com.team1.otvoo.clothes.dto.SortBy;
import com.team1.otvoo.clothes.dto.SortDirection;
import java.util.List;
import java.util.UUID;

public record ClothesAttributeDefDtoCursorResponse(
    List<ClothesAttributeDefDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    SortBy sortBy,
    SortDirection sortDirection
) {

}
