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
   * 오늘과 전날의 동일 fcstTime 데이터를 비교하여 차이값 반환
   *
   * @param items     전체 FcstItem 목록 (오늘 + 전날 포함)
   * @param fcstTime  비교 기준 예보 시각 (HHmm)
   * @param category  TMP(기온) 또는 REH(습도)
   * @param parsingUtils 카테고리별 값 파싱 유틸
   * @return Double: 오늘 값 - 전날 값 (둘 중 하나라도 없으면 null)
   */
  public static Double calculateDifference(
      List<FcstItem> items,
      String fcstTime,
      String category,
      ForecastParsingUtils parsingUtils
  ) {
    // 오늘과 전날 날짜 문자열 생성
    String today = LocalDate.now(ZONE).format(DATE_FORMAT);
    String yesterday = LocalDate.now(ZONE).minusDays(1).format(DATE_FORMAT);

    // 오늘 데이터 필터링
    Double todayValue = FcstItemUtils.parseDoubleByCategory(
        filterByDateAndTime(items, today, fcstTime),
        category,
        parsingUtils
    );

    // 전날 데이터 필터링
    Double yesterdayValue = FcstItemUtils.parseDoubleByCategory(
        filterByDateAndTime(items, yesterday, fcstTime),
        category,
        parsingUtils
    );

    if (todayValue == null || yesterdayValue == null) {
      return null;
    }

    return todayValue - yesterdayValue;
  }

  // (fcstDate + fcstTime) 매칭 -> 해당 List<FcstItem> 리턴
  private static List<FcstItem> filterByDateAndTime(List<FcstItem> items, String fcstDate, String fcstTime) {
    return items.stream()
        .filter(i -> fcstDate.equals(i.getFcstDate()) && fcstTime.equals(i.getFcstTime()))
        .collect(Collectors.toList());
  }
}
