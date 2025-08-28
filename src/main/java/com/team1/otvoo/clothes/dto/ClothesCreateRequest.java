package com.team1.otvoo.clothes.dto;

import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDto;
import com.team1.otvoo.clothes.entity.ClothesType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ClothesCreateRequest(
    @NotNull UUID ownerId,
    @NotBlank (message = "의상 이름은 필수입니다") String name,
    @NotNull ClothesType type,
    List<ClothesAttributeDto> attributes
) {

}
