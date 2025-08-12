package com.team1.otvoo.weather.batch;

import com.team1.otvoo.weather.client.WeatherClient;
import com.team1.otvoo.weather.dto.VilageFcstResponse;
import com.team1.otvoo.weather.dto.VilageFcstResponse.FcstItem;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * WeatherItemReader는 기상청 API를 호출해서 날씨 예보 데이터를 가져오고,
 * 도메인 객체 FcstItem 리스트로 변환해 Processor에 넘겨주는 Reader 컴포넌트
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class WeatherItemReader implements ItemReader<List<FcstItem>> {

  private final WeatherClient weatherClient;

  @Value("#{jobParameters['baseDate']}")
  private String baseDate;

  @Value("#{jobParameters['baseTime']}")
  private String baseTime;

  @Value("#{jobParameters['x']}")
  private int x;

  @Value("#{jobParameters['y']}")
  private int y;

  @Value("#{jobParameters['numOfRows']}")
  private int numOfRows;

  // 각 단계마다 단일 실행을 보장하기 위한 플래그
  private boolean read = false;

  @Override
  public List<FcstItem> read() throws Exception {
    if (read) {
      return null;
    }
    read = true;

    log.info("기상청 API 요청 시작: date={}, time={}, grid=({}, {})",
        baseDate, baseTime, x, y);

    // 1) 기상청 API 호출
    VilageFcstResponse apiResponse = weatherClient.getForecast(
        baseDate, baseTime, x, y);

    // 2) 응답 DTO -> FcstItem 변환
    List<FcstItem> items = apiResponse.getBody()
        .getItems()
        .getItem()
        .stream()
        .map(dto -> new FcstItem(
            dto.getBaseDate(),
            dto.getBaseTime(),
            dto.getCategory(),
            dto.getFcstDate(),
            dto.getFcstTime(),
            dto.getFcstValue(),
            dto.getNx(),
            dto.getNy()
        ))
        .collect(Collectors.toList());

    return items;
  }
}
