package com.team1.otvoo.weather.batch;

import com.team1.otvoo.weather.entity.SkyStatus;
import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.entity.WeatherTemperature;
import com.team1.otvoo.weather.repository.WeatherForecastRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WeatherForecastWriterTest {

  @Mock
  private WeatherForecastRepository weatherForecastRepository;

  @InjectMocks
  private WeatherForecastWriter writer;

  @Test
  void write_flattensAndSavesForecasts() throws Exception {
    // given
    Instant forecastedAt = Instant.parse("2025-08-28T23:00:00Z");
    Instant forecastAt = Instant.parse("2025-08-29T00:00:00Z");

    WeatherForecast forecast1 = WeatherForecast.of(forecastedAt, forecastAt, SkyStatus.CLEAR);
    WeatherTemperature temp1 = new WeatherTemperature(forecast1, 25.0, null, null, null);
    forecast1.setTemperature(temp1);

    WeatherForecast forecast2 = WeatherForecast.of(forecastedAt, forecastAt.plusSeconds(3600), SkyStatus.CLOUDY);
    WeatherTemperature temp2 = new WeatherTemperature(forecast2, 27.0, null, null, null);
    forecast2.setTemperature(temp2);

    List<WeatherForecast> forecasts = List.of(forecast1, forecast2);
    Chunk<List<WeatherForecast>> chunk = new Chunk<>(List.of(forecasts));

    // when
    writer.write(chunk);

    // then
    verify(weatherForecastRepository, times(1)).saveAll(anyList());
  }
}
