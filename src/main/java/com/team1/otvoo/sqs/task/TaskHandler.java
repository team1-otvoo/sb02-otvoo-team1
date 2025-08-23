package com.team1.otvoo.sqs.task;

import com.team1.otvoo.sqs.dto.TaskPayload;
import com.team1.otvoo.sqs.dto.TaskType;

public interface TaskHandler {
  void handle(TaskPayload payload);
  TaskType getSupportType();
}
