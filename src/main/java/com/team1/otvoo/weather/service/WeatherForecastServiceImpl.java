package com.team1.otvoo.weather.service;

import com.team1.otvoo.weather.client.KakaoLocalClient;
import com.team1.otvoo.weather.client.WeatherClient;
import com.team1.otvoo.weather.dto.VilageFcstResponse;
import com.team1.otvoo.weather.dto.VilageFcstResponse.FcstItem;
import com.team1.otvoo.weather.dto.WeatherAPILocation;
import com.team1.otvoo.weather.dto.WeatherDto;
import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.factory.WeatherForecastFactory;
import com.team1.otvoo.weather.mapper.WeatherMapper;
import com.team1.otvoo.weather.repository.WeatherForecastRepository;
import com.team1.otvoo.weather.util.GridCoordConverter;
import com.team1.otvoo.weather.util.WeatherTimeCalculator;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WeatherForecastServiceImpl implements WeatherForecastService {

  private final GridCoordConverter gridCoordConverter;
  private final KakaoLocalClient kakaoLocalClient;
  private final WeatherClient weatherClient;
  private final WeatherForecastFactory weatherForecastFactory;
  private final WeatherMapper weatherMapper;
  private final WeatherForecastRepository weatherForecastRepository;

  @Override
  public WeatherAPILocation getLocation(double longitude, double latitude) {
    GridCoordConverter.Point point = gridCoordConverter.convert(latitude, longitude);
    int x = point.getX();
    int y = point.getY();

    // Kakao API로 locationNames 조회
    List<String> locationNames = kakaoLocalClient.getRegionNames(latitude,
        longitude); // 위도, 경도 순으로 넘김

    return new WeatherAPILocation(latitude, longitude, x, y, locationNames);
  }

  @Override
  @Transactional
  public List<WeatherDto> getWeathers(double longitude, double latitude) {

    // 1. 위/경도 -> x, y 변환
    GridCoordConverter.Point point = gridCoordConverter.convert(latitude, longitude);
    int x = point.getX();
    int y = point.getY();

    // 2. 기상청 호출용 baseDate, baseTime 파라미터 설정
    String baseDate = WeatherTimeCalculator.calculateBaseDate();
    String baseTime = WeatherTimeCalculator.calculateBaseTime();

    log.info("기상청 API 호출 파라미터 - baseDate={}, baseTime={}, x={}, y={}",
        baseDate, baseTime, x, y);

    // 3. 기상청 OpenAPI 호출
    List<VilageFcstResponse.FcstItem> items =
        weatherClient.getForecast(baseDate, baseTime, x, y)
            .getResponse()
            .getBody()
            .getItems()
            .getItem();

    if (items == null || items.isEmpty()) {
      log.warn("기상청 응답 데이터 없음");
      return Collections.emptyList();
    }

    // 첫 번째 아이템의 fcstTime을 기준으로 사용
    String selectedFcstTime = items.get(0).getFcstTime();
    log.debug("선택된 fcstTime: {}", selectedFcstTime);

    // 해당 fcstTime만 필터링
    List<VilageFcstResponse.FcstItem> filteredItems = items.stream()
        .filter(i -> selectedFcstTime.equals(i.getFcstTime()))
        .collect(Collectors.toList());

    log.info("selectedFcstTime={}, filteredItems fcstTimes={}",
        selectedFcstTime,
        filteredItems.stream().map(FcstItem::getFcstTime).distinct().toList()
    );

    // 4. Kakao API로 locationNames 조회
    List<String> locationNames = kakaoLocalClient.getRegionNames(latitude,longitude);

    // 5. WeatherForecastFactory로 엔티티 변환
    List<WeatherForecast> forecasts = weatherForecastFactory.createForecasts(
        filteredItems,
        latitude,
        longitude,
        x, y,
        String.join(",", locationNames)
    );

    // 6. DB 저장
    weatherForecastRepository.saveAll(forecasts);

    // 7. UI 에 표시할 날짜 필터링 (오늘 이전 데이터 제외)
    String today = LocalDate.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    List<WeatherForecast> filtered = forecasts.stream()
        .filter(f -> {
          String fcstDate = f.getForecastAt()
              .atZone(ZoneId.of("Asia/Seoul"))
              .toLocalDate()
              .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
          return fcstDate.compareTo(today) >= 0; // 오늘 이후만
        })
        .collect(Collectors.toList());

    return filtered.stream()
        .sorted(Comparator.comparing(WeatherForecast::getForecastAt)) // 날짜 오름차순
        .map(weatherMapper::toDto)
        .collect(Collectors.toList());
  }
}
