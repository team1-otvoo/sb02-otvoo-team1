package com.team1.otvoo.weather.service;

import com.team1.otvoo.weather.dto.WeatherAPILocation;
import com.team1.otvoo.weather.dto.WeatherDto;
import java.util.List;

public interface WeatherForecastService {

  // 격자(x,y) + 행정구역명 조회
  WeatherAPILocation getLocation(double longitude, double latitude);

  List<WeatherDto> getWeathers(double longitude, double latitude);

}
