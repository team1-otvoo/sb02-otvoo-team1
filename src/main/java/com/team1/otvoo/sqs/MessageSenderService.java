package com.team1.otvoo.sqs;

import com.team1.otvoo.sqs.dto.ImageDeleteData;
import com.team1.otvoo.sqs.dto.ImageResizeData;
import com.team1.otvoo.sqs.dto.SqsMessageDto;
import com.team1.otvoo.sqs.dto.TaskType;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageSenderService {

  // Spring Cloud AWS가 자동으로 구성해주는 SQS 전송용 템플릿
  private final SqsTemplate sqsTemplate;

  @Value("${aws.sqs.queue-name}")
  private String queueName;

  // 이미지 삭제 메시지 전송
  public void sendImageDeleteMessage(String objectKey) {
    ImageDeleteData data = new ImageDeleteData(objectKey);
    SqsMessageDto messageDto = new SqsMessageDto(data);

    sqsTemplate.send(to -> to.queue(queueName).payload(messageDto));
  }

  // 이미지 리사이징 메시지 전송
  public void sendImageResizeMessage(String objectKey, int width, int height) {
    ImageResizeData data = new ImageResizeData(objectKey, width, height);
    SqsMessageDto messageDto = new SqsMessageDto(data);

    sqsTemplate.send(to -> to.queue(queueName).payload(messageDto));
  }
}
