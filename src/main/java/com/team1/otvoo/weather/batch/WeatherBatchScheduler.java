package com.team1.otvoo.weather.batch;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WeatherBatchScheduler {

  private final JobLauncher jobLauncher;
  private final Job weatherForecastJob;

  // 실행 시간: 매일 23:10
  @Scheduled(cron = "0 10 23 * * *", zone = "Asia/Seoul")
  public void runWeatherJob() throws Exception {
    JobParameters jobParameters = new JobParametersBuilder()
        .addString("baseDate", LocalDate.now(ZoneId.of("Asia/Seoul"))
            .format(DateTimeFormatter.ofPattern("yyyyMMdd")))
        .addString("baseTime", "2300")
        .addLong("timestamp", System.currentTimeMillis()) // 고유 실행 보장
        .toJobParameters();

    log.info("[WeatherBatchScheduler] 배치 실행 시작 - baseDate={}, baseTime=2300, 실행시각={}",
        LocalDate.now(ZoneId.of("Asia/Seoul"))
            .format(DateTimeFormatter.ofPattern("yyyyMMdd")),
        LocalDate.now(ZoneId.of("Asia/Seoul")).atTime(23, 10)
    );

    jobLauncher.run(weatherForecastJob, jobParameters);
  }

}
