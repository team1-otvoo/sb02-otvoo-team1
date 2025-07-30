package com.team1.otvoo.weather.dto;

import com.team1.otvoo.weather.entity.PrecipitationType;

// 강수 정보
public record PrecipitationDto(
    PrecipitationType type,
    Double amount,
    double probability
) {

}
