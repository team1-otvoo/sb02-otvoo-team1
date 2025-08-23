package com.team1.otvoo.sqs.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY, // ✅ 객체 내부에 있는 'type' 프로퍼티를 사용하도록 설정
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ImageResizeData.class, name = "IMAGE_RESIZE"),
    @JsonSubTypes.Type(value = ImageDeleteData.class, name = "IMAGE_DELETE")
})
public interface TaskPayload {
  TaskType getType(); // ✅ 모든 페이로드는 자신의 타입을 반환해야 한다는 계약 추가
}
