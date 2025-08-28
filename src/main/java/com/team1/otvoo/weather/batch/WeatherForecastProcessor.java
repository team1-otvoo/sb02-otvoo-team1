package com.team1.otvoo.weather.batch;


import com.team1.otvoo.weather.client.WeatherClient;
import com.team1.otvoo.weather.dto.VilageFcstResponse;
import com.team1.otvoo.weather.dto.VilageFcstResponse.FcstItem;
import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.entity.WeatherLocation;
import com.team1.otvoo.weather.factory.WeatherForecastFactory;
import com.team1.otvoo.weather.repository.WeatherForecastRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherForecastProcessor implements
    ItemProcessor<WeatherLocation, List<WeatherForecast>> {

  private final WeatherClient weatherClient;
  private final WeatherForecastFactory weatherForecastFactory;
  private final WeatherForecastRepository weatherForecastRepository;

  @Value("${weather.batch.base-time:2300}")
  private String baseTime;
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

  @Override
  public List<WeatherForecast> process(WeatherLocation location) {
    String baseDate = LocalDate.now(ZONE).format(DATE_FORMAT);

    log.info("배치 Processor 시작 - locationId={}, x={}, y={}, baseDate={}, baseTime={}",
        location.getId(), location.getX(), location.getY(), baseDate, baseTime);

    // 1. 기상청 API 호출
    List<FcstItem> items;
    try {
      VilageFcstResponse response = weatherClient.getForecast(baseDate, baseTime,
          location.getX(), location.getY());
      items = Optional.ofNullable(response)
          .map(r -> r.getResponse().getBody().getItems().getItem())
          .orElse(Collections.emptyList());
    } catch (Exception e) {
      log.error("기상청 API 호출 실패 - locationId={}", location.getId(), e);
      return Collections.emptyList();
    }

    if (items.isEmpty()) {
      log.warn("기상청 API 응답 데이터 없음 - locationId={}", location.getId());
      return Collections.emptyList();
    }

    // 2. TMX/TMN Map 추출
    Map<String, Double> tmxMap = extractValueByDate(items, "TMX");
    Map<String, Double> tmnMap = extractValueByDate(items, "TMN");

    // 3. Factory로 엔티티 변환
    List<WeatherForecast> forecasts = weatherForecastFactory.createForecasts(
        items,
        location,
        tmxMap,
        tmnMap
    );

    // 4. 중복 필터링 (같은 location + forecast_at + forecasted_at 조합이 이미 저장되어 있다면 제외)
    List<WeatherForecast> newForecasts = forecasts.stream()
        .filter(f ->
            !weatherForecastRepository.existsByLocationAndForecastAtAndForecastedAt(
                f.getLocation(), f.getForecastAt(), f.getForecastedAt())
        )
        .collect(Collectors.toList());

    log.info("변환된 예보 수: {}, 중복 제거 후 저장 대상: {}", forecasts.size(), newForecasts.size());

    return newForecasts;
  }

  // 하루 최고, 최저 기온 추출
  private Map<String, Double> extractValueByDate(List<FcstItem> items, String category) {
    return items.stream()
        .filter(i -> category.equals(i.getCategory()))
        .collect(Collectors.toMap(
            FcstItem::getFcstDate,
            i -> Double.parseDouble(i.getFcstValue()),
            (v1, v2) -> v1 // 중복 날짜 발생 시 첫 값 유지
        ));
  }
}
