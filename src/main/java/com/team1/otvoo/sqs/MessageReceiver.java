package com.team1.otvoo.sqs;

import com.team1.otvoo.sqs.dto.SqsMessageDto;
import com.team1.otvoo.sqs.dto.TaskType;
import com.team1.otvoo.sqs.task.TaskHandler;
import com.team1.otvoo.sqs.task.TaskHandlerFactory;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageReceiver {

  // handler 생성 객체 (실제 작업을 handler 에게 위임)
  private final TaskHandlerFactory taskHandlerFactory;

  @SqsListener("${aws.sqs.queue-name}")
  public void receiveMessage(SqsMessageDto messageDto) {

    log.info("sqs queue에서 메시지를 수신했습니다. : {}", messageDto);

    try {
      // 1. 받은 JSON 메시지를 SqsMessageDto 객체로 변환
      TaskType taskType = messageDto.data().getType();
      TaskHandler handler = taskHandlerFactory.getHandler(taskType);

      if (handler == null) {
        log.warn("지원하지 않는 TaskType 입니다. 메시지를 폐기합니다: type={}", taskType);
        return; // 지원하는 핸들러가 없으면 재시도 없이 종료
      }

      handler.handle(messageDto.data());

    } catch (Exception e) {
      // 핸들러에서 발생한 모든 예외 (DB, S3 등)를 여기서 잡습니다.
      String taskType = (messageDto != null && messageDto.data() != null) ? messageDto.data().getType().toString() : "N/A";
      log.error("SQS 메시지 처리 중 오류 발생. SQS 재시도를 요청합니다. TaskType: {}", taskType, e);

      // SQS에 처리가 실패했음을 알리기 위해 예외를 다시 던집니다.
      throw new RuntimeException("SQS message processing failed, triggering retry.", e);
    }
  }
}
