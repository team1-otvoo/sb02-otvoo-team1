package com.team1.otvoo.clothes.dto;

import com.team1.otvoo.clothes.entity.ClothesType;
import java.util.UUID;

public record ClothesSearchCondition(
    String cursor,
    UUID idAfter,
    int limit,
    ClothesType type,
    UUID ownerId
) {

}