package com.team1.otvoo.weather.util;


import com.team1.otvoo.weather.dto.VilageFcstResponse.FcstItem;
import java.util.List;

public final class FcstItemUtils {

  private FcstItemUtils() {}

  // 그룹에서 해당 category의 FcstValue를 String으로 추출
  public static String getValueByCategory(List<FcstItem> items, String category) {
    return items.stream()
        .filter(i -> category.equals(i.getCategory())) // 해당 category만 필터
        .map(FcstItem::getFcstValue)
        .findFirst()
        .orElse(null);
  }

  // category 값을 Double로 변환 (null 또는 "-"는 null 반환)
  public static Double parseDoubleByCategory(List<FcstItem> group, String category, ForecastParsingUtils parser) {
    return parser.parseDouble(getValueByCategory(group, category));
  }

  // category 값을 강수/적설 전용 Double로 변환 (특정 문자열 → 0.0 처리)
  public static Double parsePrecipOrSnowByCategory(List<FcstItem> group, String category, ForecastParsingUtils parser) {
    return parser.parsePrecipitationOrSnow(getValueByCategory(group, category));
  }

  // category 값을 정수형 코드(Integer)로 변환 (null → 0)
  public static Integer parseIntByCategory(List<FcstItem> group, String category, ForecastParsingUtils parser) {
    Double val = parser.parseDouble(getValueByCategory(group, category));
    return val != null ? val.intValue() : 0;
  }
}
