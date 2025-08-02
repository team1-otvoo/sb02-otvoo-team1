package com.team1.otvoo.weather.client;

import com.team1.otvoo.weather.client.dto.VilageFcstResponse;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherClient {

  private final RestTemplate restTemplate;

  @Value("${weather.api.base-url}")
  private String baseUrl;

  @Value("${weather.api.service-key}")
  private String serviceKey;

  public VilageFcstResponse getForecast(String baseDate, String baseTime, int nx, int ny, int numOfRows) {
    URI uri = UriComponentsBuilder.fromUriString(baseUrl)
        .queryParam("serviceKey", serviceKey)
        .queryParam("numOfRows", numOfRows)
        .queryParam("pageNo", 1)
        .queryParam("dataType", "JSON")
        .queryParam("base_date", baseDate)
        .queryParam("base_time", baseTime)
        .queryParam("nx", nx)
        .queryParam("ny", ny)
        .build(true) // 인코딩된 serviceKey 가 있다면, 다시 인코딩 하지 않게 설정
        .toUri();

    log.debug("기상청 예보 API 호출: {}", uri);

    // 1. 위에서 만든 uri로, HTTP GET 요청을 날리고
    // 2. 응답 바디(JSON)을 DTO 클래스로 변환해서 리턴받음
    return restTemplate.getForObject(uri, VilageFcstResponse.class);
  }

}
