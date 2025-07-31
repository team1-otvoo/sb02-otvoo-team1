package com.team1.otvoo.weather.mapper;

import com.team1.otvoo.weather.dto.PrecipitationDto;
import com.team1.otvoo.weather.dto.TemperatureDto;
import com.team1.otvoo.weather.dto.WeatherSummaryDto;
import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.entity.WeatherPrecipitation;
import com.team1.otvoo.weather.entity.WeatherTemperature;
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

}
