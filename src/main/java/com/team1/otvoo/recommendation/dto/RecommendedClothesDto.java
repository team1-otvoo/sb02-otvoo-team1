package com.team1.otvoo.recommendation.dto;

import com.team1.otvoo.clothes.dto.ClothesAttributeDto;
import com.team1.otvoo.clothes.entity.ClothesType;
import java.util.List;
import java.util.UUID;

public record RecommendedClothesDto(
    UUID clothesId,
    String name,
    String imageUrl,
    ClothesType type,
    List<ClothesAttributeDto> attributes
) {

}
