package com.team1.otvoo.weather.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.team1.otvoo.weather.dto.VilageFcstResponse.FcstItem;
import com.team1.otvoo.weather.entity.PrecipitationType;
import com.team1.otvoo.weather.entity.SkyStatus;
import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.entity.WeatherHumidity;
import com.team1.otvoo.weather.entity.WeatherLocation;
import com.team1.otvoo.weather.entity.WeatherPrecipitation;
import com.team1.otvoo.weather.entity.WeatherTemperature;
import com.team1.otvoo.weather.entity.WeatherWindSpeed;
import com.team1.otvoo.weather.entity.WindStrength;
import com.team1.otvoo.weather.util.ForecastParsingUtils;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WeatherForecastProcessorTest {

  @Mock
  private ForecastParsingUtils parsingUtils;

  @InjectMocks
  private WeatherForecastProcessor processor;

  @BeforeEach
  void setUp() {
    // JobParameters처럼 필드에 주입
    ReflectionTestUtils.setField(processor, "latitude", 37.5);
    ReflectionTestUtils.setField(processor, "longitude", 126.8);
    ReflectionTestUtils.setField(processor, "x", 60);
    ReflectionTestUtils.setField(processor, "y", 127);
    ReflectionTestUtils.setField(processor, "locationNames", "서울특별시,강남구,대치동");
  }

  @Test
  void process_validItems_returnsOneForecast() throws Exception {
    // given: 샘플 FcstItem 리스트 (한 시점)
    List<FcstItem> items = new ArrayList<>();
    items.add(new FcstItem("20250729", "0200", "20250729", "0200", "TMP", "21", 60, 127));
    items.add(new FcstItem("20250729", "0200", "20250729", "0200", "TMN", "18", 60, 127));
    items.add(new FcstItem("20250729", "0200", "20250729", "0200", "TMX", "25", 60, 127));
    items.add(new FcstItem("20250729", "0200", "20250729", "0200", "REH", "60", 60, 127));
    items.add(new FcstItem("20250729", "0200", "20250729", "0200", "WSD", "5", 60, 127));
    items.add(new FcstItem("20250729", "0200", "20250729", "0200", "POP", "30", 60, 127));
    items.add(new FcstItem("20250729", "0200", "20250729", "0200", "PTY", "1", 60, 127));
    items.add(new FcstItem("20250729", "0200", "20250729", "0200", "PCP", "2", 60, 127));
    items.add(new FcstItem("20250729", "0200", "20250729", "0200", "SKY", "1", 60, 127));

    // parsingUtils.parseDouble(...)와 parsePrecipitationOrSnow(...) 동작 모킹
    when(parsingUtils.parseDouble(anyString()))
        .thenAnswer(inv -> {
          String v = inv.getArgument(0);
          return v == null ? null : Double.valueOf(v);
        });
    when(parsingUtils.parsePrecipitationOrSnow(anyString()))
        .thenAnswer(inv -> {
          String v = inv.getArgument(0);
          return v == null ? 0.0 : Double.valueOf(v);
        });

    // when
    List<WeatherForecast> results = processor.process(items);

    // then
    assertThat(results).hasSize(1);
    WeatherForecast forecast = results.get(0);

    // 예상 Instant (KST 2025-07-29 02:00 → UTC 2025-07-28T17:00:00Z)
    Instant expectedInstant = ZonedDateTime.of(
        2025, 7, 29, 2, 0, 0, 0,
        ZoneId.of("Asia/Seoul")
    ).toInstant();

    // 예보 발표 시각 검증
    assertThat(forecast.getForecastedAt()).isEqualTo(expectedInstant);
    // 예보 적용 시각 검증
    assertThat(forecast.getForecastAt()).isEqualTo(expectedInstant);

    // 온도 검증
    WeatherTemperature temp = forecast.getTemperature();
    assertThat(temp.getCurrent()).isEqualTo(21.0);
    assertThat(temp.getMin()).isEqualTo(18.0);
    assertThat(temp.getMax()).isEqualTo(25.0);

    // 습도 검증
    WeatherHumidity hum = forecast.getHumidity();
    assertThat(hum.getCurrent()).isEqualTo(60.0);

    // 풍속 검증
    WeatherWindSpeed wind = forecast.getWindSpeed();
    assertThat(wind.getSpeed()).isEqualTo(5.0);
    assertThat(wind.getAsWord()).isEqualTo(WindStrength.MODERATE);

    // 강수 검증
    WeatherPrecipitation precip = forecast.getPrecipitation();
    assertThat(precip.getProbability()).isEqualTo(30.0);
    assertThat(precip.getAmount()).isEqualTo(2.0);
    assertThat(precip.getType()).isEqualTo(PrecipitationType.RAIN);

    // 하늘 상태 검증
    assertThat(forecast.getSkyStatus()).isEqualTo(SkyStatus.CLEAR);

    // 위치 검증
    WeatherLocation loc = forecast.getLocation();
    assertThat(loc.getLatitude()).isEqualTo(37.5);
    assertThat(loc.getLongitude()).isEqualTo(126.8);
    assertThat(loc.getX()).isEqualTo(60);
    assertThat(loc.getY()).isEqualTo(127);
    assertThat(loc.getLocationNames()).isEqualTo("서울특별시,강남구,대치동");
  }
}
