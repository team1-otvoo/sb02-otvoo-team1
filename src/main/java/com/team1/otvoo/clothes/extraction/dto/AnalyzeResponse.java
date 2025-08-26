package com.team1.otvoo.clothes.extraction.dto;

import com.team1.otvoo.clothes.entity.ClothesType;
import java.util.Map;

public record AnalyzeResponse(
    String name,
    String imageUrl,
    ClothesType type,
    Map<String, String> attributes
) {

}
