package com.team1.otvoo.weather.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * 기상청 API 호출 및 데이터 파싱 시 필요한 날짜·시간 계산 유틸
 */
public final class WeatherTimeCalculator {

  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

  private WeatherTimeCalculator() {}

  // 기상청 발표 시간(고정)
  private static final List<LocalTime> BASE_TIMES = Arrays.asList(
      LocalTime.of(2, 0),
      LocalTime.of(5, 0),
      LocalTime.of(8, 0),
      LocalTime.of(11, 0),
      LocalTime.of(14, 0),
      LocalTime.of(17, 0),
      LocalTime.of(20, 0),
      LocalTime.of(23, 0)
  );

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");

  /**
   * 하루 전 날짜를 yyyyMMdd 형식으로 반환
   * (API 호출 시 base_date 값)
   */
  public static String calculateBaseDate() {
    return LocalDate.now(ZONE).minusDays(1).format(DATE_FORMAT);
  }

  /**
   * 현재 시간보다 가장 가까운 과거의 발표 시간(baseTime)을 반환
   * (02, 05, 08, 11, 14, 17, 20, 23 중 선택)
   */
  public static String calculateBaseTime() {
    LocalTime currentTime = LocalTime.now(ZONE);
    LocalTime selected = BASE_TIMES.get(0);

    for (LocalTime baseTime : BASE_TIMES) {
      if (!currentTime.isBefore(baseTime)) {
        selected = baseTime;
      }
    }
    return selected.format(TIME_FORMAT);
  }

  /**
   * API 응답에서 제공된 fcstTime 목록 중 현재 시간보다 가장 가까운 과거 시각 반환
   * 1시간 간격 / 3시간 간격 모두 대응
   *
   * @param fcstTimes 기상청 API 응답에서 추출한 fcstTime 목록
   * @return HHmm 형식의 가장 가까운 과거 fcstTime, 없으면 null
   */
  public static String calculateNearestFcstTime(List<String> fcstTimes) {
    LocalTime currentTime = LocalTime.now(ZONE);
    LocalTime nearest = null;

    for (String timeStr : fcstTimes) {
      LocalTime fcstTime = LocalTime.parse(timeStr, TIME_FORMAT);
      if (!currentTime.isBefore(fcstTime)) { // 현재 시간 >= fcstTime
        if (nearest == null || fcstTime.isAfter(nearest)) {
          nearest = fcstTime;
        }
      }
    }
    return (nearest != null) ? nearest.format(TIME_FORMAT) : null;
  }
}
