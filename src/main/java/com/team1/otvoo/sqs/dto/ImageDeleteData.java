package com.team1.otvoo.sqs.dto;

public record ImageDeleteData (
    String objectKey
) implements TaskPayload {
  @Override
  public TaskType getType() { // ✅ 자신의 타입 반환
    return TaskType.IMAGE_DELETE;
  }
}