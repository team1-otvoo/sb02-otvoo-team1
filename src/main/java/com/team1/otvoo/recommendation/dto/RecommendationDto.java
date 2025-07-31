package com.team1.otvoo.recommendation.dto;

import java.util.List;
import java.util.UUID;

public record RecommendationDto(
    UUID weatherId,
    UUID userId,
    List<OotdDto> clothes
) {

}
