package com.team1.otvoo.weather.batch;


import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.repository.WeatherForecastRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherForecastWriter implements ItemWriter<List<WeatherForecast>> {

  private final WeatherForecastRepository weatherForecastRepository;

  @Override
  public void write(Chunk<? extends List<WeatherForecast>> chunk) {
    List<WeatherForecast> flatList = chunk.getItems().stream()
        .filter(list -> list != null && !list.isEmpty())
        .flatMap(List::stream)
        .toList();

    if (!flatList.isEmpty()) {
      weatherForecastRepository.saveAll(flatList);
      log.info("Writer 저장 완료 - forecasts 총 {}건", flatList.size());
    }

  }
}
