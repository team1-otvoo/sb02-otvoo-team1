package com.team1.otvoo.clothes.dto;

import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDto;
import com.team1.otvoo.clothes.entity.ClothesType;
import java.util.List;

public record ClothesUpdateRequest(
    String name,
    ClothesType type,
    List<ClothesAttributeDto> attributes
) {

}
