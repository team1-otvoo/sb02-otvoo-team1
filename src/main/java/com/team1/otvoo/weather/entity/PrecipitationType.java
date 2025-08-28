package com.team1.otvoo.weather.entity;

// 강수 형태
public enum PrecipitationType {
  NONE,       // 없음 (0)
  RAIN,       // 비 (1)
  RAIN_SNOW,  // 비/눈 (2)
  SNOW,       // 눈 (3)
  SHOWER;     // 소나기 (4)

  public static PrecipitationType fromCode(int code) {
    return switch (code) {
      case 1 -> RAIN;
      case 2 -> RAIN_SNOW;
      case 3 -> SNOW;
      case 4 -> SHOWER;
      default -> NONE;
    };
  }
}
