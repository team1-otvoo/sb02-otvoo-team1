package com.team1.otvoo.directmessage.controller;

import com.team1.otvoo.directmessage.dto.DirectMessageCreateRequest;
import com.team1.otvoo.directmessage.dto.DirectMessageDto;
import com.team1.otvoo.user.dto.UserSummary;
import com.team1.otvoo.directmessage.service.DirectMessageService;
import com.team1.otvoo.directmessage.util.DmKeyUtil;
import org.junit.jupiter.api.DisplayName;
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
  @DisplayName("sendDirectMessage - 웹소켓 메시지 정상 발송 테스트")
  public void sendDirectMessage_shouldSendMessageToCorrectDestination() {
    // given
    UUID senderId = UUID.randomUUID();
    UUID receiverId = UUID.randomUUID();

    DirectMessageCreateRequest request = new DirectMessageCreateRequest(senderId, receiverId, "안녕");

    DirectMessageDto response = mock(DirectMessageDto.class);
    UserSummary senderSummary = mock(UserSummary.class);
    UserSummary receiverSummary = mock(UserSummary.class);

    when(senderSummary.userId()).thenReturn(senderId);
    when(receiverSummary.userId()).thenReturn(receiverId);

    when(response.sender()).thenReturn(senderSummary);
    when(response.receiver()).thenReturn(receiverSummary);

    when(directMessageService.createDto(any(DirectMessageCreateRequest.class))).thenReturn(response);

    // when
    controller.sendDirectMessage(request);

    String expectedDmKey = DmKeyUtil.generate(senderId, receiverId);
    String expectedDestination = "/sub/direct-messages_" + expectedDmKey;

    // then
    verify(directMessageService).createDto(request);
    verify(messagingTemplate).convertAndSend(eq(expectedDestination), eq(response));
  }
}