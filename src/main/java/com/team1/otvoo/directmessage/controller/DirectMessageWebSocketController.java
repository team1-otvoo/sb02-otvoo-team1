package com.team1.otvoo.directmessage.controller;

import com.team1.otvoo.directmessage.dto.DirectMessageCreateRequest;
import com.team1.otvoo.directmessage.dto.DirectMessageResponse;
import com.team1.otvoo.directmessage.service.DirectMessageService;
import com.team1.otvoo.directmessage.util.DmKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@Controller
public class DirectMessageWebSocketController {

  private final SimpMessagingTemplate messagingTemplate;
  private final DirectMessageService directMessageService;

  @MessageMapping("/direct-messages_send")
  public void sendDirectMessage(DirectMessageCreateRequest request) {
    DirectMessageResponse response = directMessageService.create(request);

    String dmKey = DmKeyUtil.generate(response.senderId(), response.receiverId());

    messagingTemplate.convertAndSend("/sub/direct-messages_" + dmKey, response);
  }
}