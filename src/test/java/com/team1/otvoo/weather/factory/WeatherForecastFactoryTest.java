package com.team1.otvoo.weather.factory;

import static org.assertj.core.api.Assertions.assertThat;
import com.team1.otvoo.weather.dto.VilageFcstResponse.FcstItem;
import com.team1.otvoo.weather.entity.PrecipitationType;
import com.team1.otvoo.weather.entity.SkyStatus;
import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.entity.WindStrength;
import com.team1.otvoo.weather.util.ForecastParsingUtils;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WeatherForecastFactoryTest {

  private final ForecastParsingUtils parsingUtils = new ForecastParsingUtils();
  private final WeatherForecastFactory factory = new WeatherForecastFactory(parsingUtils);

  @Test
  void createForecasts_mapsAll_and_computesDayOverDay_perCard() {
    // given
    int nx = 60, ny = 127;
    double lat = 37.5665, lon = 126.9780;

    List<FcstItem> items = new ArrayList<>();

    // 2025-08-10 13:00 (전날)
    items.add(new FcstItem("20250810","1100","20250810","1300","TMP","25",nx,ny));
    items.add(new FcstItem("20250810","1100","20250810","1300","TMN","21",nx,ny));
    items.add(new FcstItem("20250810","1100","20250810","1300","TMX","29",nx,ny));
    items.add(new FcstItem("20250810","1100","20250810","1300","REH","70",nx,ny));
    items.add(new FcstItem("20250810","1100","20250810","1300","WSD","4.0",nx,ny));
    items.add(new FcstItem("20250810","1100","20250810","1300","POP","20",nx,ny));
    items.add(new FcstItem("20250810","1100","20250810","1300","PTY","1",nx,ny));      // RAIN
    items.add(new FcstItem("20250810","1100","20250810","1300","PCP","1.5mm",nx,ny));
    items.add(new FcstItem("20250810","1100","20250810","1300","SKY","3",nx,ny));      // MOSTLY_CLOUDY

    // 2025-08-11 13:00 (오늘)
    items.add(new FcstItem("20250811","1100","20250811","1300","TMP","30",nx,ny));
    items.add(new FcstItem("20250811","1100","20250811","1300","TMN","25",nx,ny));
    items.add(new FcstItem("20250811","1100","20250811","1300","TMX","32",nx,ny));
    items.add(new FcstItem("20250811","1100","20250811","1300","REH","60",nx,ny));
    items.add(new FcstItem("20250811","1100","20250811","1300","WSD","3.5",nx,ny));
    items.add(new FcstItem("20250811","1100","20250811","1300","POP","10",nx,ny));
    items.add(new FcstItem("20250811","1100","20250811","1300","PTY","0",nx,ny));      // NONE
    items.add(new FcstItem("20250811","1100","20250811","1300","PCP","강수없음",nx,ny));
    items.add(new FcstItem("20250811","1100","20250811","1300","SKY","1",nx,ny));      // CLEAR

    // when
    List<WeatherForecast> results = factory.createForecasts(items, lat, lon, nx, ny, "서울특별시,중구,명동");

    // then
    assertThat(results).hasSize(2);

    ZoneId KST = ZoneId.of("Asia/Seoul");
    Instant today1300 = ZonedDateTime.of(2025,8,11,13,0,0,0, KST).toInstant();
    WeatherForecast card = results.stream()
        .filter(f -> f.getForecastAt().equals(today1300))
        .findFirst()
        .orElseThrow();

    // 기본 필드
    assertThat(card.getSkyStatus()).isEqualTo(SkyStatus.CLEAR);
    assertThat(card.getPrecipitation().getType()).isEqualTo(PrecipitationType.NONE);
    assertThat(card.getPrecipitation().getProbability()).isEqualTo(10.0);
    assertThat(card.getPrecipitation().getAmount()).isEqualTo(0.0);

    // 온도
    assertThat(card.getTemperature().getCurrent()).isEqualTo(30.0);
    assertThat(card.getTemperature().getMin()).isEqualTo(25.0);
    assertThat(card.getTemperature().getMax()).isEqualTo(32.0);
    // 전일대비(같은 시각)
    assertThat(card.getTemperature().getComparedToDayBefore()).isEqualTo(5.0); // 30 - 25

    // 습도
    assertThat(card.getHumidity().getCurrent()).isEqualTo(60.0);
    assertThat(card.getHumidity().getComparedToDayBefore()).isEqualTo(-10.0); // 60 - 70

    // 바람
    assertThat(card.getWindSpeed().getSpeed()).isEqualTo(3.5);
    assertThat(card.getWindSpeed().getAsWord()).isEqualTo(WindStrength.WEAK);

    // 위치
    assertThat(card.getLocation().getLatitude()).isEqualTo(lat);
    assertThat(card.getLocation().getLongitude()).isEqualTo(lon);
    assertThat(card.getLocation().getX()).isEqualTo(nx);
    assertThat(card.getLocation().getY()).isEqualTo(ny);
    assertThat(card.getLocation().getLocationNames()).contains("서울특별시","중구","명동");
  }
}
