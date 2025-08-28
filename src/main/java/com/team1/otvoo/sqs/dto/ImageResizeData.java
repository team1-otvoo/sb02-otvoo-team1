package com.team1.otvoo.sqs.dto;

public record ImageResizeData(
    String objectKey,
    int width,
    int height
) implements TaskPayload {
  @Override
  public TaskType getType() { // ✅ 자신의 타입 반환
    return TaskType.IMAGE_RESIZE;
  }
}