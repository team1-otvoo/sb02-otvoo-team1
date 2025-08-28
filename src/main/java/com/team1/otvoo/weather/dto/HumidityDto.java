package com.team1.otvoo.weather.dto;

// 습도 정보
public record HumidityDto(
    double current,
    double comparedToDayBefore
) {

}
