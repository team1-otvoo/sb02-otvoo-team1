package com.team1.otvoo.sqs.task;

import com.team1.otvoo.sqs.dto.ImageResizeData;
import com.team1.otvoo.sqs.dto.TaskPayload;
import com.team1.otvoo.sqs.dto.TaskType;
import com.team1.otvoo.storage.ImageProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageResizeHandler implements TaskHandler{
  private final ImageProcessingService imageProcessingService;

  @Override
  public void handle(TaskPayload payload) {
    ImageResizeData data = (ImageResizeData) payload;
    imageProcessingService.resizeAndOverwrite(data.objectKey(), data.width(), data.height());
  }

  @Override
  public TaskType getSupportType() {
    return TaskType.IMAGE_RESIZE;
  }
}
