package com.team1.otvoo.clothes.dto;

import java.util.List;

public record ClothesAttributeDefUpdateRequest(
    String name,
    List<String> selectableValues
) {

}
