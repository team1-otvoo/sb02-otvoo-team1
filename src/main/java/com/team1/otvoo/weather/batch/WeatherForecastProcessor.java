package com.team1.otvoo.weather.batch;


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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");
  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

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

  private final ForecastParsingUtils parsingUtils;

  @Override
  public List<WeatherForecast> process(List<FcstItem> items) {
    log.debug("Processor 입력 데이터 개수: {}", items.size());

    // 1. fcstDate + fcstTime 기준으로 그룹핑
    Map<ForecastKey, List<FcstItem>> grouped = items.stream()
        .collect(Collectors.groupingBy(
            item -> new ForecastKey(item.getFcstDate(), item.getFcstTime())
        ));

    List<WeatherForecast> results = new ArrayList<>();

    // 2. FcstItemUtils로 카테고리별 값 파싱
    for (Map.Entry<ForecastKey, List<FcstItem>> entry : grouped.entrySet()) {
      ForecastKey key = entry.getKey();
      List<FcstItem> group = entry.getValue();

      // - 온도 -
      Double tmp = FcstItemUtils.parseDoubleByCategory(group, "TMP", parsingUtils);
      Double tmn = FcstItemUtils.parseDoubleByCategory(group, "TMN", parsingUtils);
      Double tmx = FcstItemUtils.parseDoubleByCategory(group, "TMX", parsingUtils);

      // - 습도 -
      Double reh = FcstItemUtils.parseDoubleByCategory(group, "REH", parsingUtils);

      // - 풍속 -
      Double wsd = FcstItemUtils.parseDoubleByCategory(group, "WSD", parsingUtils);
      WindStrength windStrength = (wsd != null)
          ? WindStrength.fromSpeed(wsd)
          : WindStrength.WEAK;

      // - 강수 확률 -
      Double pop = FcstItemUtils.parseDoubleByCategory(group, "POP", parsingUtils);

      // - 강수 형태 & 양 -
      int ptyCode = FcstItemUtils.parseIntByCategory(group, "PTY", parsingUtils);
      PrecipitationType precipitationType = PrecipitationType.fromCode(ptyCode);
      Double pcp = FcstItemUtils.parsePrecipOrSnowByCategory(group, "PCP", parsingUtils);

      // — 하늘 상태 —
      int skyCode = FcstItemUtils.parseIntByCategory(group, "SKY", parsingUtils);
      SkyStatus skyStatus = SkyStatus.fromCode(skyCode);

      // 예보 시각 계산 (forecastedAt -> 예보 발표알, forecastAt -> 예보 적용일)
      FcstItem first = group.get(0);
      Instant forecastedAt = LocalDateTime.of(
          LocalDate.parse(first.getBaseDate(), DATE_FORMAT),
          LocalTime.parse(first.getBaseTime(), TIME_FORMAT)
      ).atZone(ZONE).toInstant();
      Instant forecastAt = LocalDateTime.of(
          LocalDate.parse(key.fcstDate(), DATE_FORMAT),
          LocalTime.parse(key.fcstTime(), TIME_FORMAT)
      ).atZone(ZONE).toInstant();

      // 엔티티 생성 및 연동
      WeatherForecast forecast = WeatherForecast.of(forecastedAt, forecastAt, skyStatus);
      List<String> regions = Arrays.asList(locationNames.split(","));
      WeatherLocation location = new WeatherLocation(
          forecast,
          x, y,
          latitude, longitude,
          regions
      );
      forecast.setLocation(location);

      // comparedToDayBefore -> 추후 계산 로직 반영 예정
      WeatherTemperature temperature = new WeatherTemperature(forecast, tmp, tmn, tmx, 0.0);
      forecast.setTemperature(temperature);

      // comparedToDayBefore -> 추후 계산 로직 반영 예정
      WeatherHumidity humidity = new WeatherHumidity(forecast, reh, 0.0);
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
