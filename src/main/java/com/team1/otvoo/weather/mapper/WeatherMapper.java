package com.team1.otvoo.weather.mapper;

import com.team1.otvoo.user.dto.Location;
import com.team1.otvoo.weather.dto.PrecipitationDto;
import com.team1.otvoo.weather.dto.TemperatureDto;
import com.team1.otvoo.weather.dto.WeatherSummaryDto;
import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.entity.WeatherLocation;
import com.team1.otvoo.weather.entity.WeatherPrecipitation;
import com.team1.otvoo.weather.entity.WeatherTemperature;
import java.util.Arrays;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WeatherMapper {
  @Mapping(source = "id", target = "weatherId")
  @Mapping(source = "precipitation", target = "precipitation")
  @Mapping(source = "temperature", target = "temperature")
  WeatherSummaryDto toSummaryDto(WeatherForecast weatherForecast);
  PrecipitationDto toPrecipitationDto(WeatherPrecipitation entity);
  TemperatureDto toTemperatureDto(WeatherTemperature entity);

  @Mapping(target = "locationNames", expression = "java(splitLocationNames(entity.getLocationNames()))")
  Location toLocation(WeatherLocation entity);

  default List<String> splitLocationNames(String locationNames) {
    if (locationNames == null || locationNames.isBlank()) {
      return List.of();
    }
    return Arrays.stream(locationNames.split(","))
        .map(String::trim) // 공백 제거
        .toList();
  }
}
