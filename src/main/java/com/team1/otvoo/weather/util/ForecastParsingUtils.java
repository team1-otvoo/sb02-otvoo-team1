package com.team1.otvoo.weather.util;

import org.springframework.stereotype.Component;

@Component
public final class ForecastParsingUtils {

  /**
   * 안전한 Double 파싱 - null 또는 "-" → null
   */
  public Double parseDouble(String value) {
    if (value == null || "-".equals(value)) {
      return null;
    }
    try {
      return Double.valueOf(value.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * 강수량 및 적설량 파싱 - null 또는 "-"/"강수없음"/"적설없음"/"0"→0.0
   */
  public Double parsePrecipitationOrSnow(String value) {
    if (value == null || "-".equals(value)
        || "강수없음".equals(value) || "적설없음".equals(value)
        || "0".equals(value) || "0.0".equals(value)) {
      return 0.0;
    }
    try {
      return Double.valueOf(
          value.replace("mm", "")
              .replace("cm", "")
              .replace("이상", "")
              .split("~")[0]
              .trim()
      );
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
