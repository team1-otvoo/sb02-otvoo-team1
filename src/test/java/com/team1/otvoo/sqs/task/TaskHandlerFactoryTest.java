package com.team1.otvoo.sqs.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.team1.otvoo.sqs.dto.TaskType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TaskHandlerFactory 단위 테스트")
class TaskHandlerFactoryTest {

  private TaskHandler mockDeleteHandler;
  private TaskHandler mockResizeHandler;

  @BeforeEach
  void setUp() {
    // 각 테스트에서 사용할 Mock 핸들러들을 미리 생성
    mockDeleteHandler = mock(ImageDeleteHandler.class);
    mockResizeHandler = mock(ImageResizeHandler.class);

    // 각 핸들러가 어떤 TaskType을 지원하는지 설정
    when(mockDeleteHandler.getSupportType()).thenReturn(TaskType.IMAGE_DELETE);
    when(mockResizeHandler.getSupportType()).thenReturn(TaskType.IMAGE_RESIZE);
  }

  @Test
  @DisplayName("생성 시 TaskHandler 리스트를 TaskType을 키로 하는 맵으로 변환한다")
  void should_initializeHandlersMap_onCreation() {
    // given
    List<TaskHandler> handlerList = List.of(mockDeleteHandler, mockResizeHandler);

    // when
    TaskHandlerFactory factory = new TaskHandlerFactory(handlerList);

    // then
    // 각 TaskType에 맞는 정확한 핸들러 인스턴스를 반환하는지 확인
    assertThat(factory.getHandler(TaskType.IMAGE_DELETE)).isSameAs(mockDeleteHandler);
    assertThat(factory.getHandler(TaskType.IMAGE_RESIZE)).isSameAs(mockResizeHandler);
  }

  @Test
  @DisplayName("지원하지 않는 TaskType을 조회하면 null을 반환한다")
  void should_returnNull_when_handlerForTypeDoesNotExist() {
    // given
    List<TaskHandler> handlerList = List.of(mockDeleteHandler);
    TaskHandlerFactory factory = new TaskHandlerFactory(handlerList);

    // when
    // 맵에 없는 TaskType으로 조회를 시도
    TaskHandler result = factory.getHandler(TaskType.IMAGE_RESIZE);

    // then
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("동일한 TaskType을 지원하는 핸들러가 중복되면 IllegalStateException을 던진다")
  void should_throwIllegalStateException_when_duplicateHandlersExist() {
    // given
    // 두 번째 핸들러도 IMAGE_DELETE 타입을 지원하도록 설정 (중복 상황)
    TaskHandler anotherDeleteHandler = mock(ImageDeleteHandler.class);
    when(anotherDeleteHandler.getSupportType()).thenReturn(TaskType.IMAGE_DELETE);

    List<TaskHandler> handlerListWithDuplicates = List.of(mockDeleteHandler, anotherDeleteHandler);

    // when & then
    // 팩토리 생성 시점에 Collectors.toUnmodifiableMap에 의해 예외가 발생하는지 검증
    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
      new TaskHandlerFactory(handlerListWithDuplicates);
    });

    // 예외 메시지에 "Duplicate key"가 포함되어 있는지 확인하여 더 확실하게 검증
    assertThat(exception.getMessage()).contains("Duplicate key");
  }
}