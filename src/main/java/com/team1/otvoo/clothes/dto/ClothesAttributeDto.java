package com.team1.otvoo.clothes.dto;

import java.util.UUID;

public record ClothesAttributeDto(
    UUID definitionId,
    String value
) {

}
