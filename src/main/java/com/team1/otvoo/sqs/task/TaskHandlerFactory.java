package com.team1.otvoo.sqs.task;

import com.team1.otvoo.sqs.dto.TaskType;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TaskHandlerFactory {
  private final Map<TaskType, TaskHandler> handlers;

  // Spring이 알아서 TaskHandler 타입의 Bean들을 모두 주입 (생성자 주입)
  public TaskHandlerFactory(List<TaskHandler> handlerList) {
    handlers = handlerList.stream()
        .collect(Collectors.toUnmodifiableMap(TaskHandler::getSupportType, Function.identity()));
  }

  public TaskHandler getHandler(TaskType type) {
    return handlers.get(type);
  }
}
