package com.team1.otvoo.weather.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team1.otvoo.weather.entity.QWeatherForecast;
import com.team1.otvoo.weather.entity.QWeatherHumidity;
import com.team1.otvoo.weather.entity.QWeatherLocation;
import com.team1.otvoo.weather.entity.QWeatherPrecipitation;
import com.team1.otvoo.weather.entity.QWeatherTemperature;
import com.team1.otvoo.weather.entity.QWeatherWindSpeed;
import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.repository.custom.WeatherForecastRepositoryCustom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WeatherForecastRepositoryImpl implements WeatherForecastRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<WeatherForecast> findLatest5ByLocation(int x, int y) {
    QWeatherForecast forecast = QWeatherForecast.weatherForecast;
    QWeatherLocation location = QWeatherLocation.weatherLocation;
    QWeatherTemperature temperature = QWeatherTemperature.weatherTemperature;
    QWeatherHumidity humidity = QWeatherHumidity.weatherHumidity;
    QWeatherPrecipitation precipitation = QWeatherPrecipitation.weatherPrecipitation;
    QWeatherWindSpeed windSpeed = QWeatherWindSpeed.weatherWindSpeed;

    // 오늘 자정 00:00 (KST 기준)을 Instant로 변환하여 아래 필터링에 이용
    Instant todayStart = LocalDate.now()
        .atStartOfDay(ZoneId.of("Asia/Seoul"))
        .toInstant();

    return queryFactory
        .selectFrom(forecast)
        .join(forecast.location, location).fetchJoin()
        .join(forecast.temperature, temperature).fetchJoin()
        .join(forecast.humidity, humidity).fetchJoin()
        .join(forecast.precipitation, precipitation).fetchJoin()
        .join(forecast.windSpeed, windSpeed).fetchJoin()
        .where(
            location.x.eq(x),
            location.y.eq(y),
            forecast.forecastAt.goe(todayStart) // 오늘부터 미래 예보만 필터링
        )
        .orderBy(forecast.forecastAt.asc())
        .limit(5)
        .fetch();
  }
}
