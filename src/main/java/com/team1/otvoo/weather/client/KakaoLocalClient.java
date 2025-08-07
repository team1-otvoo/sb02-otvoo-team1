package com.team1.otvoo.weather.client;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.weather.dto.KakaoRegionResponse;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoLocalClient {

  private final RestTemplate restTemplate;

  @Value("${kakao.local.api.base-url}")
  private String kakaoBaseUrl;

  @Value("${kakao.local.api.service-key}")
  private String kakaoApiKey;

  public List<String> getRegionNames(double latitude, double longitude) {
    // 1. API 요청 URL 생성
    URI uri = UriComponentsBuilder.fromUriString(kakaoBaseUrl)
        .queryParam("x", longitude)
        .queryParam("y", latitude)
        .build()
        .toUri();

    // 2. 요청 헤더 구성 (Authorization)
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "KakaoAK " + kakaoApiKey);

    // 3. HttpEntity 생성 (헤더만 담음)
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    // 4. 카카오 API 요청 실행
    ResponseEntity<KakaoRegionResponse> response;
    try {
      response = restTemplate.exchange(
          uri,
          HttpMethod.GET,
          entity,
          KakaoRegionResponse.class // 응답을 KakaoRegionResponse로 파싱
      );
    } catch (Exception e) {
      log.error("카카오 위치 API 호출 실패", e);
      throw new RestException(ErrorCode.EXTERNAL_API_ERROR);
    }

    if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
      log.warn("카카오 API 응답 실패: status={}, body={}", response.getStatusCode(), response.getBody());
      throw new RestException(ErrorCode.EXTERNAL_API_ERROR);
    }

    // 5. 응답의 documents 배열에서 첫 번째 결과만 추출
    KakaoRegionResponse.Document doc = response.getBody()
        .documents()
        .stream()
        .findFirst()
        .orElseThrow(() -> new RestException(ErrorCode.EXTERNAL_API_EMPTY_RESPONSE));

    return List.of(doc.region1(), doc.region2(), doc.region3());
  }

}
