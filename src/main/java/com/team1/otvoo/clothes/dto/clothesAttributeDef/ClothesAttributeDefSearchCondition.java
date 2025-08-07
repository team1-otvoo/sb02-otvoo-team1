package com.team1.otvoo.clothes.dto.clothesAttributeDef;

import com.team1.otvoo.clothes.dto.SortBy;
import com.team1.otvoo.clothes.dto.SortDirection;
import java.util.UUID;

public record ClothesAttributeDefSearchCondition(
    String cursor,
    UUID idAfter,
    int limit,
    SortBy sortBy,
    SortDirection sortDirection,
    String keywordLike
) {

}