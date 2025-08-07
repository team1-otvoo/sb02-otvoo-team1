package com.team1.otvoo.clothes.dto;

import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDto;
import com.team1.otvoo.clothes.entity.ClothesType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClothesDto(
    UUID id,
    UUID ownerId,
    String name,
    String imageUrl,
    ClothesType type,
    List<ClothesAttributeDto> attributes,
    Instant createdAt
) {

}
