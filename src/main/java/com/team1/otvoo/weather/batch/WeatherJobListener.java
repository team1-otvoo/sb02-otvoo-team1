package com.team1.otvoo.weather.batch;

import com.team1.otvoo.weather.entity.WeatherLocation;
import com.team1.otvoo.weather.repository.WeatherLocationRepository;
import com.team1.otvoo.weather.service.WeatherChangeDetector;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherJobListener implements JobExecutionListener {

  private final WeatherLocationRepository weatherLocationRepository;
  private final WeatherChangeDetector weatherChangeDetector;

  @Override
  public void beforeJob(JobExecution jobExecution) {
    log.info(">>> [WeatherBatch] Job 시작 - jobName={}, startTime={}",
        jobExecution.getJobInstance().getJobName(),
        jobExecution.getStartTime());
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
      long totalWrites = jobExecution.getStepExecutions().stream()
          .mapToLong(StepExecution::getWriteCount)
          .sum();

      log.info(">>> [WeatherBatch] Job 완료 - jobName={}, endTime={}, 총 처리건수={}",
          jobExecution.getJobInstance().getJobName(),
          jobExecution.getEndTime(),
          totalWrites
      );

      // 저장이 끝난 후 Detector 실행
      List<WeatherLocation> locations = weatherLocationRepository.findAll();
      for (WeatherLocation location : locations) {
        try {
          weatherChangeDetector.detectChanges(location);
        } catch (Exception e) {
          log.error("날씨 변화 탐지 실패 - locationId={}", location.getId(), e);
        }
      }

    } else {
      log.error(">>> [WeatherBatch] Job 실패 - jobName={}, status={}, exitStatus={}",
          jobExecution.getJobInstance().getJobName(),
          jobExecution.getStatus(),
          jobExecution.getExitStatus().getExitDescription()
      );
    }
  }
}
