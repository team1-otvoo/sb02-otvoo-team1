package com.team1.otvoo.clothes.dto;

import com.team1.otvoo.clothes.entity.ClothesType;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record OotdDto(
    UUID clothesId,
    String name,
    String imageUrl,
    ClothesType type,
    List<ClothesAttributeWithDefDto> attributes
) {

}
