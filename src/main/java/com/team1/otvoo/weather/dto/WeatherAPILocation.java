package com.team1.otvoo.weather.dto;

import java.util.List;

// 기상청 단기예보 API 위치 정보
public record WeatherAPILocation(
    double latitude,
    double longitude,
    int x,
    int y,
    List<String> locationNames
) {

}
