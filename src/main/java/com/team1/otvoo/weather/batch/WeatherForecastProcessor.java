package com.team1.otvoo.weather.batch;


import com.team1.otvoo.weather.dto.VilageFcstResponse.FcstItem;
import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.factory.WeatherForecastFactory;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * 날씨 API에서 받아온 원시 데이터(List<FcstItem>)를
 * 도메인의 WeatherForecast 엔티티(및 연관된 서브 엔티티)로 변환하는 책임
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class WeatherForecastProcessor implements
    ItemProcessor<List<FcstItem>, List<WeatherForecast>> {

  @Value("#{jobParameters['latitude']}")
  private double latitude;

  @Value("#{jobParameters['longitude']}")
  private double longitude;

  @Value("#{jobParameters['x']}")
  private int x;

  @Value("#{jobParameters['y']}")
  private int y;

  @Value("#{jobParameters['locationNames']}")
  private String locationNames;

  private final WeatherForecastFactory weatherForecastFactory;

  @Override
  public List<WeatherForecast> process(List<FcstItem> items) {

    // 임시 TMX/TMN 값 — 빈 Map
    Map<String, Double> tmxMap = Collections.emptyMap();
    Map<String, Double> tmnMap = Collections.emptyMap();

    log.debug("Processor 입력 데이터 개수: {}", items.size());
    return weatherForecastFactory.createForecasts(
        items,
        latitude,
        longitude,
        x,
        y,
        locationNames,
        tmxMap,
        tmnMap
    );
  }
}
