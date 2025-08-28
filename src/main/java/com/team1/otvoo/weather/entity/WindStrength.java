package com.team1.otvoo.weather.entity;

// 풍속 정성 표현
public enum WindStrength {
  WEAK,
  MODERATE,
  STRONG;

  public static WindStrength fromSpeed(double speed) {
    if (speed < 4) return WEAK;
    if (speed < 9) return MODERATE;
    return STRONG;
  }
}
