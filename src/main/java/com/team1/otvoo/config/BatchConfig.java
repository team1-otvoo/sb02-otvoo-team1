package com.team1.otvoo.config;

import com.team1.otvoo.weather.batch.WeatherForecastProcessor;
import com.team1.otvoo.weather.batch.WeatherLocationReader;
import com.team1.otvoo.weather.batch.WeatherForecastWriter;
import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.entity.WeatherLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

  private final WeatherLocationReader weatherLocationReader;
  private final WeatherForecastProcessor weatherForecastProcessor;
  private final WeatherForecastWriter weatherForecastWriter;

  // Job 정의: weatherBatchJob
  @Bean
  public Job weatherBatchJob() {
    return new JobBuilder("weatherBatchJob", jobRepository)
        .start(weatherForecastStep())  // Step을 연결
        .build();
  }

  // Step 정의: weatherForecastStep
  // Reader -> WeatherLocation 반환
  // Processor -> WeatherForecast 반환
  @Bean
  public Step weatherForecastStep() {
    return new StepBuilder("weatherForecastStep", jobRepository)
        .<WeatherLocation, WeatherForecast>chunk(10, transactionManager) // 10개의 위치 단위로 처리 후 Writer 실행
        .reader(weatherLocationReader)
        .processor(weatherForecastProcessor)
        .writer(weatherForecastWriter)
        .build();
  }

}
