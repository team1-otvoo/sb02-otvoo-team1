package com.team1.otvoo.directmessage.controller;

import com.team1.otvoo.directmessage.dto.DirectMessageCreateRequest;
import com.team1.otvoo.directmessage.dto.DirectMessageResponse;
import com.team1.otvoo.directmessage.service.DirectMessageService;
import com.team1.otvoo.directmessage.util.DmKeyUtil;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
public class DirectMessageWebSocketControllerTest {

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @Mock
  private DirectMessageService directMessageService;

  @InjectMocks
  private DirectMessageWebSocketController controller;

  @Test
  public void sendDirectMessage_shouldSendMessageToCorrectDestination() {
    // given
    UUID senderId = UUID.randomUUID();
    UUID receiverId = UUID.randomUUID();

    DirectMessageCreateRequest request = new DirectMessageCreateRequest(senderId, receiverId, "hello");

    DirectMessageResponse response = new DirectMessageResponse(senderId, receiverId, "hello", null);

    when(directMessageService.create(any(DirectMessageCreateRequest.class))).thenReturn(response);

    // when
    controller.sendDirectMessage(request);

    String expectedDmKey = DmKeyUtil.generate(senderId, receiverId);
    String expectedDestination = "/sub/direct-messages_" + expectedDmKey;

    // then
    verify(directMessageService).create(request);
    verify(messagingTemplate).convertAndSend(eq(expectedDestination), eq(response));
  }
}
