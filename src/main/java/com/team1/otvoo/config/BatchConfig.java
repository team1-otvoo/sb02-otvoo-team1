package com.team1.otvoo.config;

import com.team1.otvoo.weather.batch.WeatherForecastProcessor;
import com.team1.otvoo.weather.batch.WeatherItemReader;
import com.team1.otvoo.weather.batch.WeatherItemWriter;
import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.entity.WeatherLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
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
  private final WeatherItemWriter writer;

  // Job 정의: weatherBatchJob
  @Bean
  public Job weatherBatchJob() {
    return new JobBuilder("weatherBatchJob", jobRepository)
        .start(weatherForecastStep())  // Step을 연결
        .build();
  }

  // Step 정의: weatherForecastStep
  @Bean
  public Step weatherForecastStep() {
    return new StepBuilder("weatherForecastStep", jobRepository)
        .<WeatherLocation, WeatherForecast>chunk(10, transactionManager)
        .reader(weatherLocationReader())
        .processor(weatherForecastProcessor())
        .writer(weatherForecastWriter())
        .build();
  }

  @Bean
  public ItemReader<WeatherLocation> weatherLocationReader() {
    return null;
  }

  @Bean
  public ItemProcessor<WeatherLocation, WeatherForecast> weatherForecastProcessor() {
    return null;
  }

  @Bean
  public ItemWriter<WeatherForecast> weatherForecastWriter() {
    return null;
  }


}
