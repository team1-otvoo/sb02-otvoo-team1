package com.team1.otvoo.clothes.dto;

import java.util.List;

public record ClothesAttributeDefCreateRequest(
    String name,
    List<String> selectableValues
) {

}
