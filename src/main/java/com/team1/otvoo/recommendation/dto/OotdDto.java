package com.team1.otvoo.recommendation.dto;

import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDto;
import com.team1.otvoo.clothes.entity.ClothesType;
import java.util.List;
import java.util.UUID;

public record OotdDto(
    UUID clothesId,
    String name,
    String imageUrl,
    ClothesType type,
    List<ClothesAttributeDto> attributes
) {

}
