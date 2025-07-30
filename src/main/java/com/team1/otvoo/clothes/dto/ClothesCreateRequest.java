package com.team1.otvoo.clothes.dto;

import com.team1.otvoo.clothes.entity.ClothesType;
import java.util.List;
import java.util.UUID;

public record ClothesCreateRequest(
    UUID ownerId,
    String name,
    ClothesType type,
    List<ClothesAttributeDto> attributes
) {

}
