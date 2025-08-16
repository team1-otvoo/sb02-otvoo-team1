package com.team1.otvoo.weather.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WeatherJobListener implements JobExecutionListener {

  @Override
  public void beforeJob(JobExecution jobExecution) {
    log.info(">>> [WeatherBatch] Job 시작 - jobName={}, startTime={}",
        jobExecution.getJobInstance().getJobName(),
        jobExecution.getStartTime());
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
      log.info(">>> [WeatherBatch] Job 완료 - jobName={}, endTime={}, 총 처리건수={}",
          jobExecution.getJobInstance().getJobName(),
          jobExecution.getEndTime(),
          jobExecution.getStepExecutions().stream()
              .mapToLong(StepExecution::getWriteCount)
              .sum()
      );
    } else {
      log.error(">>> [WeatherBatch] Job 실패 - jobName={}, status={}, exitStatus={}",
          jobExecution.getJobInstance().getJobName(),
          jobExecution.getStatus(),
          jobExecution.getExitStatus().getExitDescription()
      );
    }
  }
}
