package com.team1.otvoo.sqs.task;

import com.team1.otvoo.sqs.dto.ImageDeleteData;
import com.team1.otvoo.sqs.dto.TaskPayload;
import com.team1.otvoo.sqs.dto.TaskType;
import com.team1.otvoo.storage.S3ImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageDeleteHandler implements TaskHandler {
  private final S3ImageStorage s3ImageStorage;

  @Override
  public void handle(TaskPayload payload) {
    ImageDeleteData data = (ImageDeleteData) payload;
    s3ImageStorage.delete(data.objectKey());
  }

  @Override
  public TaskType getSupportType() {
    return TaskType.IMAGE_DELETE;
  }
}