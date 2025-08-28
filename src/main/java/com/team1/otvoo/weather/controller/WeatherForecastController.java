package com.team1.otvoo.weather.controller;

import com.team1.otvoo.weather.dto.WeatherAPILocation;
import com.team1.otvoo.weather.dto.WeatherDto;
import com.team1.otvoo.weather.service.WeatherForecastService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weathers")
@RequiredArgsConstructor
public class WeatherForecastController {

  private final WeatherForecastService weatherForecastService;

  // 좌표에 해당하는 행정 구역명 조회
  @GetMapping("/location")
  public ResponseEntity<WeatherAPILocation> getLocation(
      @RequestParam double latitude,
      @RequestParam double longitude
  ) {
    WeatherAPILocation location = weatherForecastService.getLocation(longitude, latitude);
    return ResponseEntity.ok(location);
  }

  // 좌표에 해당하는 날씨 정보 조회
  @GetMapping
  public ResponseEntity<List<WeatherDto>> getWeathers(
      @RequestParam double latitude,
      @RequestParam double longitude
  ) {
    List<WeatherDto> weathers = weatherForecastService.getWeathers(longitude, latitude);
    return ResponseEntity.ok(weathers);
  }
}
