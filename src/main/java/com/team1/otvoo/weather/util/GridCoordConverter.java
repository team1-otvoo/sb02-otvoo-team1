package com.team1.otvoo.weather.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.springframework.stereotype.Component;

@Component
public class GridCoordConverter {

  // 고정된 기상청 설정값 (공식 수식 기반)
  private static final double RE = 6371.00877;  // 지구 반경 (km)
  private static final double GRID = 5.0;       // 격자 간격 (km)
  private static final double SLAT1 = 30.0;     // 표준위도1
  private static final double SLAT2 = 60.0;     // 표준위도2
  private static final double OLON = 126.0;     // 기준점 경도
  private static final double OLAT = 38.0;      // 기준점 위도
  private static final double XO = 43;          // 기준점 X 좌표 (GRID 단위)
  private static final double YO = 136;         // 기준점 Y 좌표 (GRID 단위)

  public Point convert(double lat, double lng) {
    double DEGRAD = Math.PI / 180.0;

    double re = RE / GRID;
    double slat1 = SLAT1 * DEGRAD;
    double slat2 = SLAT2 * DEGRAD;
    double olon = OLON * DEGRAD;
    double olat = OLAT * DEGRAD;

    double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
    sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);

    double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
    sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;

    double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
    ro = re * sf / Math.pow(ro, sn);

    double ra = Math.tan(Math.PI * 0.25 + (lat) * DEGRAD * 0.5);
    ra = re * sf / Math.pow(ra, sn);

    double theta = lng * DEGRAD - olon;
    if (theta > Math.PI) theta -= 2.0 * Math.PI;
    if (theta < -Math.PI) theta += 2.0 * Math.PI;
    theta *= sn;

    int x = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
    int y = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);

    return new Point(x, y);
  }

  @Getter
  @AllArgsConstructor
  @ToString
  public static class Point {
    private final int x;
    private final int y;
  }
}
