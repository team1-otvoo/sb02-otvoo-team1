package com.team1.otvoo.weather.factory;

import static org.assertj.core.api.Assertions.assertThat;

import com.team1.otvoo.config.QueryDslConfig;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@EntityScan(basePackages = "com.team1.otvoo.weather.entity")
@Import({WeatherForecastFactory.class, ForecastParsingUtils.class, QueryDslConfig.class, WeatherForecastPersistenceTest.JpaSliceConfig.class})
class WeatherForecastPersistenceTest {

  // weather 리포지토리만 스캔
  @TestConfiguration
  @EnableJpaRepositories(basePackages = "com.team1.otvoo.weather.repository")
  static class JpaSliceConfig {}

  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HHmm");

  @Autowired
  TestEntityManager em;

  @Autowired
  WeatherForecastFactory factory;

  @Test
  void save_and_load_forecast_with_children_and_dayoverday() {
    // given
    int nx = 60;
    int ny = 127;
    double lat = 37.5665;
    double lon = 126.9780;

    List<FcstItem> items = new ArrayList<>();

    // 오늘(2025-08-11) 13:00 카드용 데이터
    items.add(new FcstItem("20250811", "1100", "20250811", "1300", "TMP", "30", nx, ny));
    items.add(new FcstItem("20250811", "1100", "20250811", "1300", "TMN", "25", nx, ny));
    items.add(new FcstItem("20250811", "1100", "20250811", "1300", "TMX", "32", nx, ny));
    items.add(new FcstItem("20250811", "1100", "20250811", "1300", "REH", "60", nx, ny));
    items.add(new FcstItem("20250811", "1100", "20250811", "1300", "WSD", "3.5", nx, ny));
    items.add(new FcstItem("20250811", "1100", "20250811", "1300", "POP", "10", nx, ny));
    items.add(new FcstItem("20250811", "1100", "20250811", "1300", "PTY", "0", nx, ny));
    items.add(new FcstItem("20250811", "1100", "20250811", "1300", "PCP", "강수없음", nx, ny));
    items.add(new FcstItem("20250811", "1100", "20250811", "1300", "SKY", "1", nx, ny));

    // 전날(2025-08-10) 13:00 전일대비용 데이터
    items.add(new FcstItem("20250810", "1100", "20250810", "1300", "TMP", "25", nx, ny));
    items.add(new FcstItem("20250810", "1100", "20250810", "1300", "TMN", "21", nx, ny));
    items.add(new FcstItem("20250810", "1100", "20250810", "1300", "TMX", "29", nx, ny));
    items.add(new FcstItem("20250810", "1100", "20250810", "1300", "REH", "70", nx, ny));
    items.add(new FcstItem("20250810", "1100", "20250810", "1300", "WSD", "4.0", nx, ny));
    items.add(new FcstItem("20250810", "1100", "20250810", "1300", "POP", "20", nx, ny));
    items.add(new FcstItem("20250810", "1100", "20250810", "1300", "PTY", "1", nx, ny));
    items.add(new FcstItem("20250810", "1100", "20250810", "1300", "PCP", "1.5mm", nx, ny));
    items.add(new FcstItem("20250810", "1100", "20250810", "1300", "SKY", "3", nx, ny));

    // when: Factory로 엔티티 구성
    List<WeatherForecast> forecasts =
        factory.createForecasts(items, lat, lon, nx, ny, "서울특별시,중구,명동");

    // 부모만 persist해도 CascadeType.ALL로 자식 5개가 같이 저장됨
    forecasts.forEach(em::persist);
    em.flush();
    em.clear();

    // then: 오늘 13:00(KST)의 카드 엔티티 로드 후 전부 검증
    Instant expectedForecastAt = LocalDateTime.of(2025, 8, 11, 13, 0).atZone(ZONE).toInstant();

    WeatherForecast loaded = em.getEntityManager()
        .createQuery("""
            select f from WeatherForecast f
            where f.forecastAt = :fa
            """, WeatherForecast.class)
        .setParameter("fa", expectedForecastAt)
        .getSingleResult();

    // 하늘 상태
    assertThat(loaded.getSkyStatus()).isEqualTo(SkyStatus.CLEAR);

    // 온도
    WeatherTemperature t = loaded.getTemperature();
    assertThat(t).isNotNull();
    assertThat(t.getCurrent()).isEqualTo(30.0);
    assertThat(t.getMin()).isEqualTo(25.0);
    assertThat(t.getMax()).isEqualTo(32.0);
    // 전일 대비: 30 - 25 = +5
    assertThat(t.getComparedToDayBefore()).isEqualTo(5.0);

    // 습도
    WeatherHumidity h = loaded.getHumidity();
    assertThat(h).isNotNull();
    assertThat(h.getCurrent()).isEqualTo(60.0);
    // 전일 대비: 60 - 70 = -10
    assertThat(h.getComparedToDayBefore()).isEqualTo(-10.0);

    // 풍속
    WeatherWindSpeed w = loaded.getWindSpeed();
    assertThat(w).isNotNull();
    assertThat(w.getSpeed()).isEqualTo(3.5);
    assertThat(w.getAsWord()).isEqualTo(WindStrength.WEAK);

    // 강수
    WeatherPrecipitation p = loaded.getPrecipitation();
    assertThat(p).isNotNull();
    assertThat(p.getProbability()).isEqualTo(10.0);
    assertThat(p.getAmount()).isEqualTo(0.0); // "강수없음" → 0.0
    assertThat(p.getType()).isEqualTo(PrecipitationType.NONE); // PTY=0

    // 위치
    WeatherLocation loc = loaded.getLocation();
    assertThat(loc).isNotNull();
    assertThat(loc.getLatitude()).isEqualTo(lat);
    assertThat(loc.getLongitude()).isEqualTo(lon);
    assertThat(loc.getX()).isEqualTo(nx);
    assertThat(loc.getY()).isEqualTo(ny);
    // 지역명 저장 형태가 리스트/문자열 중 어떤 구현이든 null 아님만 체크
    assertThat(loc.getLocationNames()).isNotNull();
  }
}
