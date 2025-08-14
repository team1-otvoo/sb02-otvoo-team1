package com.team1.otvoo.clothes.event;

import com.team1.otvoo.clothes.entity.ClothesAttributeDefinition;

public record ClothesAttributeEvent(
    String methodType,
    ClothesAttributeDefinition savedClothesAttributeDefinition
) {

}
