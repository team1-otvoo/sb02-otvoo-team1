package com.team1.otvoo.weather.mapper;

import com.team1.otvoo.user.dto.Location;
import com.team1.otvoo.weather.dto.HumidityDto;
import com.team1.otvoo.weather.dto.PrecipitationDto;
import com.team1.otvoo.weather.dto.TemperatureDto;
import com.team1.otvoo.weather.dto.WeatherAPILocation;
import com.team1.otvoo.weather.dto.WeatherDto;
import com.team1.otvoo.weather.dto.WeatherSummaryDto;
import com.team1.otvoo.weather.dto.WindSpeedDto;
import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.entity.WeatherHumidity;
import com.team1.otvoo.weather.entity.WeatherLocation;
import com.team1.otvoo.weather.entity.WeatherLocation;
import com.team1.otvoo.weather.entity.WeatherPrecipitation;
import com.team1.otvoo.weather.entity.WeatherTemperature;
import com.team1.otvoo.weather.entity.WeatherWindSpeed;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = {ZoneId.class, LocalDateTime.class})
public interface WeatherMapper {

  // KST 고정 ZoneId
  ZoneId KST = ZoneId.of("Asia/Seoul");

  // Weather -> Summary DTO (피드용)
  @Mapping(source = "id", target = "weatherId")
  WeatherSummaryDto toSummaryDto(WeatherForecast forecast);

  // Weather -> Full DTO(상세 API용)
  @Mapping(target = "forecastedAt", expression = "java(toKstLocalDateTime(forecast.getForecastedAt()))")
  @Mapping(target = "forecastAt", expression = "java(toKstLocalDateTime(forecast.getForecastAt()))")
  WeatherDto toDto(WeatherForecast forecast);

  // 단일 하위 엔티티 -> DTO 매핑
  PrecipitationDto toPrecipitationDto(WeatherPrecipitation entity);
  TemperatureDto toTemperatureDto(WeatherTemperature entity);
  HumidityDto toHumidityDto(WeatherHumidity entity);
  WindSpeedDto toWindSpeedDto(WeatherWindSpeed entity);

  // List<WeatherForecast> -> List<WeatherSummaryDto>
  List<WeatherSummaryDto> toSummaryList(List<WeatherForecast> forecasts);

  @Mapping(target = "locationNames", expression = "java(splitLocationNames(entity.getLocationNames()))" )
  WeatherAPILocation toWeatherAPILocation(WeatherLocation entity);

  @Mapping(target = "locationNames", expression = "java(splitLocationNames(entity.getLocationNames()))")
  Location toLocation(WeatherLocation entity);

  default List<String> splitLocationNames(String locationNames) {
    if (locationNames == null || locationNames.isBlank()) {
      return Collections.emptyList();
    }
    return Arrays.stream(locationNames.split(","))
        .map(String::trim) // 공백 제거
        .toList();
  }

  // Instant → LocalDateTime(KST) 변환
  default LocalDateTime toKstLocalDateTime(Instant instant) {
    if (instant == null) return null;
    return LocalDateTime.ofInstant(instant, KST);
  }
}
