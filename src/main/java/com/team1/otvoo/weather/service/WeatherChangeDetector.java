package com.team1.otvoo.weather.service;

import com.team1.otvoo.notification.service.SendNotificationService;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.repository.ProfileRepository;
import com.team1.otvoo.weather.entity.PrecipitationType;
import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.entity.WeatherLocation;
import com.team1.otvoo.weather.repository.WeatherForecastRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherChangeDetector {

  private final WeatherForecastRepository weatherForecastRepository;
  private final ProfileRepository profileRepository;
  private final SendNotificationService sendNotificationService;

  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

  /**
   * 오늘 발표 vs 어제 발표 기준으로 날씨 변화 탐지
   * @param location WeatherLocation
   */
  @Transactional
  public void detectChanges(WeatherLocation location) {
    LocalDate tomorrow = LocalDate.now(ZONE).plusDays(1);

    // 발표 시각 (오늘 23시, 어제 23시)
    Instant todayForecastedAt = tomorrow.minusDays(1)
        .atTime(23, 0).atZone(ZONE).toInstant();
    Instant yesterdayForecastedAt = tomorrow.minusDays(2)
        .atTime(23, 0).atZone(ZONE).toInstant();

    log.info("변화 탐지 시작 - locationId={}, tomorrow={}, todayFcstAt={}, yesterdayFcstAt={}",
        location.getId(), tomorrow, todayForecastedAt, yesterdayForecastedAt);

    // baseTime 오늘, 이전 날짜의 각각
    // 내일 하루치 forecast_at 전부 조회(0000 ~ 2300)
    List<WeatherForecast> todayForecasts =
        weatherForecastRepository.findByLocationAndForecastedAtAndForecastDate(
            location, todayForecastedAt, tomorrow);
    List<WeatherForecast> yesterdayForecasts =
        weatherForecastRepository.findByLocationAndForecastedAtAndForecastDate(
            location, yesterdayForecastedAt, tomorrow);

    if (todayForecasts.isEmpty() || yesterdayForecasts.isEmpty()) {
      log.warn("비교할 데이터 없음 - locationId={}", location.getId());
      return;
    }

    // forecast_at 기준 매핑
    // Key: forecastAt(예보시간), Value: WeatherForecast
    Map<Instant, WeatherForecast> yesterdayMap = yesterdayForecasts.stream()
        .collect(Collectors.toMap(WeatherForecast::getForecastAt, f -> f));

    // 오늘 데이터의 예보 시간(forecastAt)을 기준으로 어제 맵에서 동일 시간을 꺼냄
    // ex) 오늘 발표의 8월 17일 15시 예보 <-> 어제 발표의 8월 17일 15시 예보
    for (WeatherForecast todayFcst : todayForecasts) {
      WeatherForecast yesterdayFcst = yesterdayMap.get(todayFcst.getForecastAt());
      if (yesterdayFcst == null) continue;

      detectTemperatureChange(location, yesterdayFcst, todayFcst);
      detectPrecipitationChange(location, yesterdayFcst, todayFcst);
    }
  }

  private void detectTemperatureChange(WeatherLocation location,
      WeatherForecast yesterday,
      WeatherForecast today) {
    if (today.getTemperature() == null || yesterday.getTemperature() == null) return;

    double diff = today.getTemperature().getCurrent() - yesterday.getTemperature().getCurrent();
    if (Math.abs(diff) >= 3) {
      String fcstTimeStr = formatFcstTime(today.getForecastAt());
      String content = String.format(
          "%s 예보가 기존 동일 예보 대비 %+,.0f℃ 변했습니다.",
          fcstTimeStr, diff
      );
      notifyUsers(location, "기온 변화 알림", content);
    }
  }

  private void detectPrecipitationChange(WeatherLocation location,
      WeatherForecast yesterday,
      WeatherForecast today) {
    if (today.getPrecipitation() == null || yesterday.getPrecipitation() == null) return;

    PrecipitationType prev = yesterday.getPrecipitation().getType();
    PrecipitationType curr = today.getPrecipitation().getType();

    // 조건: (없음 -> 값) 이거나 (값 -> 없음) 일때만
    boolean changed = (prev == PrecipitationType.NONE && curr != PrecipitationType.NONE)
        || (prev != PrecipitationType.NONE && curr == PrecipitationType.NONE);

    if (changed) {
      String fcstTimeStr = formatFcstTime(today.getForecastAt());
      String content = String.format(
          "%s 예보가 기존 동일 예보 대비 강수형태가 %s -> %s 로 변했습니다.",
          fcstTimeStr, prev.name(), curr.name()
      );
      notifyUsers(location, "강수 형태 변화 알림", content);
    }
  }

  private void notifyUsers(WeatherLocation location, String title, String content) {
    // 해당 지역을 프로필에 등록한 유저 조회
    List<Profile> profiles = profileRepository.findByLocation(location);

    for (Profile profile : profiles) {
      sendNotificationService.sendWeatherForecastNotification(
          profile.getUser(),
          title,
          content
      );
    }
  }

  private String formatFcstTime(Instant forecastAt) {
    LocalDateTime ldt = forecastAt.atZone(ZONE).toLocalDateTime();
    return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH시"));
  }
}
