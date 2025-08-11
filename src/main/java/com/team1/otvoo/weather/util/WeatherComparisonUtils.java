package com.team1.otvoo.weather.util;

import com.team1.otvoo.weather.dto.VilageFcstResponse.FcstItem;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public final class WeatherComparisonUtils {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

  private WeatherComparisonUtils() {}

  /**
   * 주어진 targetFcstDate(YYYYMMDD)와 그 전날의 동일 fcstTime(HHmm)을 비교하여
   * category(TMP 또는 REH)의 차이값을 반환
   *
   * @param items 전체 FcstItem 목록 (여러 일자의 데이터 포함)
   * @param targetFcstDate 비교 기준 날짜(YYYYMMDD) - 이 날짜와 전날을 비교
   * @param fcstTime 비교 기준 예보 시각(HHmm)
   * @param category TMP(기온) 또는 REH(습도)
   * @param parsingUtils 카테고리별 값 파싱 유틸
   * @return Double: (targetFcstDate 값) - (전날 값). 둘 중 하나라도 없으면 null
   */
  public static Double calculateDifferenceForDate(
      List<FcstItem> items,
      String targetFcstDate,
      String fcstTime,
      String category,
      ForecastParsingUtils parsingUtils
  ) {
    // 방어: 필수 인자 누락 시 null 반환 (NPE 방지)
    if (items == null || items.isEmpty()) return null;
    if (targetFcstDate == null || targetFcstDate.isBlank()) return null;
    if (fcstTime == null || fcstTime.isBlank()) return null;
    if (category == null || category.isBlank()) return null;

    // targetFcstDate의 전날 계산
    String prevDate = LocalDate.parse(targetFcstDate, DATE_FORMAT)
        .minusDays(1)
        .format(DATE_FORMAT);

    // target 날짜 값
    Double targetValue = FcstItemUtils.parseDoubleByCategory(
        filterByDateAndTime(items, targetFcstDate, fcstTime),
        category,
        parsingUtils
    );

    // 전날 값
    Double prevValue = FcstItemUtils.parseDoubleByCategory(
        filterByDateAndTime(items, prevDate, fcstTime),
        category,
        parsingUtils
    );

    if (targetValue == null || prevValue == null) return null;
    return targetValue - prevValue;
  }

  /**
   * 오늘(LocalDate.now(ZONE))과 전날의 동일 fcstTime을 비교하여 차이값 반환.
   * (호환용 래퍼) — 기존 호출부가 있으면 유지되도록 남겨둔다.
   */
  public static Double calculateDifference(
      List<FcstItem> items,
      String fcstTime,
      String category,
      ForecastParsingUtils parsingUtils
  ) {
    if (fcstTime == null || fcstTime.isBlank()) return null;

    String today = LocalDate.now(ZONE).format(DATE_FORMAT);
    return calculateDifferenceForDate(items, today, fcstTime, category, parsingUtils);
  }

  // (fcstDate + fcstTime) 매칭 -> 해당 List<FcstItem> 리턴 (인자 null-safe는 상위에서 보장)
  private static List<FcstItem> filterByDateAndTime(
      List<FcstItem> items,
      String fcstDate,
      String fcstTime
  ) {
    return items.stream()
        // 상수.equals(변수) 형태라 변수 쪽이 null이어도 NPE 없음
        .filter(i -> fcstDate.equals(i.getFcstDate()) && fcstTime.equals(i.getFcstTime()))
        .collect(Collectors.toList());
  }
}
