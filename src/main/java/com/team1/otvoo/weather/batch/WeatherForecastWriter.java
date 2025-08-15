package com.team1.otvoo.weather.batch;


import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.repository.WeatherForecastRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Writer: Processor에서 생성된 WeatherForecast 엔티티 리스트를 DB에 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherForecastWriter implements ItemWriter<List<WeatherForecast>> {

  private final WeatherForecastRepository weatherForecastRepository;

  @Override
  public void write(Chunk<? extends List<WeatherForecast>> items) throws Exception {
    // List<List<WeatherForecast>> 형태의 items를 단일 List로 변환
    List<WeatherForecast> forecasts = items.getItems().stream()
        .flatMap(List::stream)
        .collect(Collectors.toList());

    // 저장 (CascadeType.ALL 로 하위 연관 엔티티도 함께 저장)
    weatherForecastRepository.saveAll(forecasts);
  }
}
