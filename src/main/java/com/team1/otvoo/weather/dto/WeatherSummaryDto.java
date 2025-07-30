package com.team1.otvoo.weather.dto;

import com.team1.otvoo.weather.entity.SkyStatus;
import java.util.UUID;

// 날씨 요약 정보
public record WeatherSummaryDto(
    UUID weatherId,
    SkyStatus skyStatus,
    PrecipitationDto precipitation,
    TemperatureDto temperature
) {

}
