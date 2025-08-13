package com.team1.otvoo.weather.factory;

import com.team1.otvoo.weather.dto.VilageFcstResponse.FcstItem;
import com.team1.otvoo.weather.entity.PrecipitationType;
import com.team1.otvoo.weather.entity.SkyStatus;
import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.entity.WeatherHumidity;
import com.team1.otvoo.weather.entity.WeatherLocation;
import com.team1.otvoo.weather.entity.WeatherPrecipitation;
import com.team1.otvoo.weather.entity.WeatherTemperature;
import com.team1.otvoo.weather.entity.WeatherWindSpeed;
import com.team1.otvoo.weather.entity.WindStrength;
import com.team1.otvoo.weather.util.FcstItemUtils;
import com.team1.otvoo.weather.util.ForecastParsingUtils;
import com.team1.otvoo.weather.util.WeatherComparisonUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherForecastFactory {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");
  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

  private final ForecastParsingUtils parsingUtils;

  /**
   * FcstItem 원시 데이터를 기반으로 WeatherForecast 엔티티 목록을 생성
   */
  public List<WeatherForecast> createForecasts(
      List<FcstItem> items,
      double latitude,
      double longitude,
      int x,
      int y,
      String locationNames,
      Map<String, Double> tmxMap,
      Map<String, Double> tmnMap
  ) {
    log.debug("Factory 입력 데이터 개수: {}", items.size());

    // 1. fcstDate + fcstTime 기준으로 그룹핑
    Map<ForecastKey, List<FcstItem>> grouped = items.stream()
        .collect(Collectors.groupingBy(
            item -> new ForecastKey(item.getFcstDate(), item.getFcstTime())
        ));

    List<WeatherForecast> results = new ArrayList<>();

    // 2. 카테고리별 값 파싱 및 엔티티 생성
    for (Map.Entry<ForecastKey, List<FcstItem>> entry : grouped.entrySet()) {
      ForecastKey key = entry.getKey();
      List<FcstItem> group = entry.getValue();

      // 온도
      Double tmp = FcstItemUtils.parseDoubleByCategory(group, "TMP", parsingUtils);
      Double tmn = FcstItemUtils.parseDoubleByCategory(group, "TMN", parsingUtils);
      Double tmx = FcstItemUtils.parseDoubleByCategory(group, "TMX", parsingUtils);

      // group에서 TMX/TMN이 없으면 파라미터로 받은 Map에서 가져오기
      if (tmn == null) {
        tmn = tmnMap.getOrDefault(key.fcstDate(), null);
      }
      if (tmx == null) {
        tmx = tmxMap.getOrDefault(key.fcstDate(), null);
      }

      // 온도(TMP) 전일 대비 계산
      Double tmpDiff = WeatherComparisonUtils.calculateDifferenceForDate(
          items, key.fcstDate(), key.fcstTime(), "TMP", parsingUtils
      );

      // 습도
      Double reh = FcstItemUtils.parseDoubleByCategory(group, "REH", parsingUtils);

      // 카드별 fcstDate/fcstTime으로 전일대비 계산
      Double rehDiff = WeatherComparisonUtils.calculateDifferenceForDate(
          items, key.fcstDate(), key.fcstTime(), "REH", parsingUtils
      );

      // 풍속
      Double wsd = FcstItemUtils.parseDoubleByCategory(group, "WSD", parsingUtils);
      WindStrength windStrength = (wsd != null)
          ? WindStrength.fromSpeed(wsd)
          : WindStrength.WEAK;

      // 강수 확률
      Double pop = FcstItemUtils.parseDoubleByCategory(group, "POP", parsingUtils);
      if (pop != null) {
        pop = pop / 100.0;
      }

      // 강수 형태 & 양
      int ptyCode = FcstItemUtils.parseIntByCategory(group, "PTY", parsingUtils);
      PrecipitationType precipitationType = PrecipitationType.fromCode(ptyCode);
      Double pcp = FcstItemUtils.parsePrecipOrSnowByCategory(group, "PCP", parsingUtils);

      // 하늘 상태
      int skyCode = FcstItemUtils.parseIntByCategory(group, "SKY", parsingUtils);
      SkyStatus skyStatus = SkyStatus.fromCode(skyCode);

      // 예보 시각 계산
      FcstItem first = group.get(0);
      log.info(">>> group.get(0) fcstDate={}, fcstTime={}, baseDate={}, baseTime={}",
          first.getFcstDate(), first.getFcstTime(),
          first.getBaseDate(), first.getBaseTime());

      Instant forecastedAt = LocalDateTime.of(
          LocalDate.parse(first.getBaseDate(), DATE_FORMAT),
          LocalTime.parse(first.getBaseTime(), TIME_FORMAT)
      ).atZone(ZONE).toInstant();
      Instant forecastAt = LocalDateTime.of(
          LocalDate.parse(key.fcstDate(), DATE_FORMAT),
          LocalTime.parse(key.fcstTime(), TIME_FORMAT)
      ).atZone(ZONE).toInstant();

      // 엔티티 생성
      WeatherForecast forecast = WeatherForecast.of(forecastedAt, forecastAt, skyStatus);
      List<String> regions = (locationNames != null && !locationNames.isBlank())
          ? Arrays.asList(locationNames.split(","))
          : Collections.emptyList();

      WeatherLocation location = new WeatherLocation(
          forecast,
          x, y,
          latitude, longitude,
          regions
      );
      forecast.setLocation(location);

      WeatherTemperature temperature = new WeatherTemperature(forecast, tmp, tmn, tmx, tmpDiff);
      forecast.setTemperature(temperature);

      WeatherHumidity humidity = reh == null  && rehDiff == null ? new WeatherHumidity(forecast, 0, 0.0) : new WeatherHumidity(forecast, reh, rehDiff);
      forecast.setHumidity(humidity);

      WeatherPrecipitation precipitation = new WeatherPrecipitation(forecast, precipitationType, pcp, pop);
      forecast.setPrecipitation(precipitation);

      WeatherWindSpeed windSpeed = new WeatherWindSpeed(forecast, wsd, windStrength);
      forecast.setWindSpeed(windSpeed);

      results.add(forecast);
    }

    return results;
  }

  // 날짜 + 시간 조합 Key
  public record ForecastKey(String fcstDate, String fcstTime) {
    public String asString() {
      return fcstDate + fcstTime;
    }
  }
}
