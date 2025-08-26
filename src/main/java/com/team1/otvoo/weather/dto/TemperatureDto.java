package com.team1.otvoo.weather.dto;

// 온도 정보
public record TemperatureDto(
    double current,
    Double comparedToDayBefore,
    Double min,
    Double max
) {

}
