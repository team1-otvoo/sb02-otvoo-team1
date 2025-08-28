package com.team1.otvoo.weather.entity;

// 하늘 상태
public enum SkyStatus {
  CLEAR,           // 맑음 (기상청 코드 1)
  MOSTLY_CLOUDY,   // 구름 많음 (기상청 코드 3)
  CLOUDY;          // 흐림 (기상청 코드 4)

  public static SkyStatus fromCode(int code) {
    return switch (code) {
      case 1 -> CLEAR;
      case 3 -> MOSTLY_CLOUDY;
      case 4 -> CLOUDY;
      default -> CLEAR; // 기본값: 맑음
    };
  }
}
