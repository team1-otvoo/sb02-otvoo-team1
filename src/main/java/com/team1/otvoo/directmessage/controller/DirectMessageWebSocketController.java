package com.team1.otvoo.directmessage.controller;

import com.team1.otvoo.directmessage.dto.DirectMessageCreateRequest;
import com.team1.otvoo.directmessage.dto.DirectMessageDto;
import com.team1.otvoo.directmessage.dto.DirectMessageResponse;
import com.team1.otvoo.directmessage.service.DirectMessageService;
import com.team1.otvoo.directmessage.util.DmKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@RequiredArgsConstructor
@Controller
public class DirectMessageWebSocketController {

  private final SimpMessagingTemplate messagingTemplate;
  private final DirectMessageService directMessageService;

  @MessageMapping("/direct-messages_send")
  public void sendDirectMessage(DirectMessageCreateRequest request) {
    log.info("✅ 웹소켓 메시지 수신: senderId={}, receiverId={}", request.senderId(), request.receiverId());

    DirectMessageDto response = directMessageService.createDto(request);

    String dmKey = DmKeyUtil.generate(response.sender().userId(), response.receiver().userId());
    log.info("✅ 웹소켓 DM Key 생성됨: {}", dmKey);

    messagingTemplate.convertAndSend("/sub/direct-messages_" + dmKey, response);

    log.info("✅ 메시지 발송 완료: DM Key={}", dmKey);
  }
}