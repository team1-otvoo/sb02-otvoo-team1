package com.team1.otvoo.config;

import com.team1.otvoo.weather.batch.WeatherForecastProcessor;
import com.team1.otvoo.weather.dto.VilageFcstResponse.FcstItem;
import com.team1.otvoo.weather.entity.WeatherForecast;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final WeatherForecastProcessor processor;
  private final WeatherItemReader reader;
  private final WeatherItemWrtier writer;

  // Job 정의: weatherJob
  @Bean
  public Job weatherJob() {
    return new JobBuilder("weatherJob", jobRepository)
        .start(weatherStep())  // Step을 연결
        .build();
  }

  // Step 정의: weatherStep
  @Bean
  public Step weatherStep() {
    SimpleStepBuilder<List<FcstItem>, List<WeatherForecast>> builder =
        new StepBuilder("weatherStep", jobRepository)
            .<List<FcstItem>, List<WeatherForecast>>chunk(1, transactionManager);

    return builder
        .reader(reader)             // C단계에서 구현할 ItemReader
        .processor(processor)       // 이미 구현된 WeatherForecastProcessor
        .writer(writer)             // E단계에서 구현할 ItemWriter
        .faultTolerant()            // H단계: Retry/Skip 정책 추가 가능
        .build();
  }
}
