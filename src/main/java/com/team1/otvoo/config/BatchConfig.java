package com.team1.otvoo.config;

import com.team1.otvoo.weather.batch.WeatherForecastProcessor;
import com.team1.otvoo.weather.batch.WeatherJobListener;
import com.team1.otvoo.weather.batch.WeatherLocationReader;
import com.team1.otvoo.weather.batch.WeatherForecastWriter;
import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.entity.WeatherLocation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

  private final WeatherLocationReader weatherLocationReader;
  private final WeatherForecastProcessor weatherForecastProcessor;
  private final WeatherForecastWriter weatherForecastWriter;

  // Job 정의: weatherForecastJob
  @Bean
  public Job weatherForecastJob(WeatherJobListener weatherJobListener) {
    return new JobBuilder("weatherForecastJob", jobRepository)
        .listener(weatherJobListener)
        .start(weatherForecastStep())  // Step을 연결
        .build();
  }

  // Step 정의: weatherForecastStep
  // Reader -> WeatherLocation 반환
  // Processor -> List<WeatherForecast> 반환
  @Bean
  public Step weatherForecastStep() {
    return new StepBuilder("weatherForecastStep", jobRepository)
        .<WeatherLocation, List<WeatherForecast>>chunk(10, transactionManager) // 10개의 위치 단위로 처리 후 Writer 실행
        .reader(weatherLocationReader)
        .processor(weatherForecastProcessor)
        .writer(weatherForecastWriter)

        // fault tolerant 설정 시작
        .faultTolerant()
        // Retry
        .retryLimit(3)                                  // 최대 3회 재시도
        .retry(RestClientException.class)               // API 호출 실패에 대해서
        .retry(ResourceAccessException.class)           // 네트워크 타임아웃 등

        // Skip
        .skipLimit(10)                                  // 최대 10건까지 Skip 허용
        .skip(RestClientException.class)                // 재시도해도 안 되면 skip
        .skip(ResourceAccessException.class)
        .build();
  }

}
