package com.team1.otvoo.weather.dto;

import com.team1.otvoo.weather.entity.SkyStatus;
import java.time.Instant;
import java.util.UUID;

// 날씨 정보
public record WeatherDto(
    UUID id,
    Instant forecastedAt,
    Instant forecastAt,
    WeatherAPILocation location,
    SkyStatus skyStatus,
    PrecipitationDto precipitation,
    HumidityDto humidity,
    TemperatureDto temperature,
    WindSpeedDto windSpeed
) {

}
