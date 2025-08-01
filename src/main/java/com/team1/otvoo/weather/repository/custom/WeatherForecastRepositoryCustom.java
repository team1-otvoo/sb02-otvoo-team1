package com.team1.otvoo.weather.repository.custom;

import com.team1.otvoo.weather.entity.WeatherForecast;
import java.util.List;

public interface WeatherForecastRepositoryCustom {
  List<WeatherForecast> findLatest5ByLocation(int x, int y);
}
