package com.team1.otvoo.weather.dto;

import com.team1.otvoo.weather.entity.WindStrength;

// 풍속 정보
public record WindSpeedDto(
    double speed,
    WindStrength asWord
) {

}
