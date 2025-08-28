package com.team1.otvoo.clothes.extraction.dto;

import java.util.Map;

public record ClassificationResult(
    Map<String,String> recognized,
    Map<String,String> unknowns
) {

}
