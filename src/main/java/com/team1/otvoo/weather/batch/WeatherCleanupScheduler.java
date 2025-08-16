package com.team1.otvoo.weather.batch;

import com.team1.otvoo.weather.repository.WeatherForecastRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class WeatherCleanupScheduler {

  private final WeatherForecastRepository weatherForecastRepository;

  // 매일 00:30에 실행
  // forecasted_at < D-2 자정 데이터 자동 삭제
  @Scheduled(cron = "0 30 0 * * *", zone = "Asia/Seoul")
  public void cleanupOldForecasts() {
    Instant threshold = LocalDate.now(ZoneId.of("Asia/Seoul"))
        .minusDays(2)
        .atStartOfDay(ZoneId.of("Asia/Seoul"))
        .toInstant();

    weatherForecastRepository.deleteOlderThan(threshold);

    log.info("Clean-up 완료: {} 이전 forecastedAt 데이터 삭제", threshold);
  }
}
