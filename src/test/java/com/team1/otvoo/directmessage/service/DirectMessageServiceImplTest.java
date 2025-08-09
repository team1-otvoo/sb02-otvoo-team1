package com.team1.otvoo.directmessage.service;

import com.team1.otvoo.directmessage.dto.DirectMessageCreateRequest;
import com.team1.otvoo.directmessage.dto.DirectMessageDto;
import com.team1.otvoo.directmessage.dto.DirectMessageDtoCursorResponse;
import com.team1.otvoo.directmessage.entity.DirectMessage;
import com.team1.otvoo.directmessage.repository.DirectMessageRepository;
import com.team1.otvoo.directmessage.repository.DirectMessageRepositoryCustom;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.user.dto.UserSummary;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DirectMessageServiceImplTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private DirectMessageRepository directMessageRepository;

  @Mock
  private DirectMessageRepositoryCustom directMessageRepositoryCustom;

  @InjectMocks
  private DirectMessageServiceImpl directMessageService;

  private UUID senderId;
  private UUID receiverId;
  private User sender;
  private User receiver;
  private DirectMessageCreateRequest defaultRequest;
  private Instant now;

  @BeforeEach
  void setup() {
    senderId = UUID.randomUUID();
    sender = User.builder()
        .email("sender@test.com")
        .password("password")
        .build();
    ReflectionTestUtils.setField(sender, "id", senderId);

    receiverId = UUID.randomUUID();
    receiver = User.builder()
        .email("receiver@test.com")
        .password("password")
        .build();
    ReflectionTestUtils.setField(receiver, "id", receiverId);

    defaultRequest = new DirectMessageCreateRequest(senderId, receiverId, "기본 메시지");

    now = Instant.now();
  }

  @Test
  @DisplayName("다이렉트 메시지 생성 성공")
  void create_Success() {
    // given
    UUID messageId = UUID.randomUUID();
    String content = defaultRequest.content();

    DirectMessage savedMessage = DirectMessage.builder()
        .id(messageId)
        .sender(sender)
        .receiver(receiver)
        .content(content)
        .createdAt(now)
        .build();

    DirectMessageDto expectedDto = new DirectMessageDto(
        messageId,
        now,
        new UserSummary(senderId, null, null),
        new UserSummary(receiverId, null, null),
        content
    );

    when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
    when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
    when(directMessageRepository.save(any())).thenReturn(savedMessage);
    when(directMessageRepositoryCustom.findByIdWithUserSummaries(any())).thenReturn(expectedDto);

    // when
    DirectMessageDto actual = directMessageService.createDto(defaultRequest);

    // then
    assertEquals(expectedDto, actual);
    verify(directMessageRepository).save(any());
  }

  @Test
  @DisplayName("다이렉트 메시지 생성 실패 - 발신자 없음")
  void create_SenderNotFound() {
    // given
    when(userRepository.findById(senderId)).thenReturn(Optional.empty());

    // when & then
    assertThrows(RestException.class, () -> directMessageService.createDto(defaultRequest));

    verify(userRepository, never()).findById(receiverId);
  }

  @Test
  @DisplayName("다이렉트 메시지 생성 실패 - 수신자 없음")
  void create_ReceiverNotFound() {
    // given
    when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
    when(userRepository.findById(receiverId)).thenReturn(Optional.empty());

    // when & then
    assertThrows(RestException.class, () -> directMessageService.createDto(defaultRequest));
  }

  @Test
  @DisplayName("다이렉트 메시지 조회 성공")
  void getDirectMessageByUserId_Success() {
    // given
    UUID userId = senderId;
    String cursorStr = null;
    String idAfterStr = null;
    int limit = 2;

    UUID dmId = UUID.randomUUID();
    Instant createdAt = now;

    DirectMessageDto dmDto = new DirectMessageDto(
        dmId,
        createdAt,
        new UserSummary(senderId, null, null),
        new UserSummary(receiverId, null, null),
        "테스트 메시지"
    );

    List<DirectMessageDto> dmDtoList = List.of(dmDto);

    when(userRepository.findById(userId)).thenReturn(Optional.of(sender));
    when(directMessageRepositoryCustom.findDirectMessagesWithCursor(userId, null, null, limit + 1)).thenReturn(dmDtoList);
    when(directMessageRepositoryCustom.countDirectMessagesByUserId(userId)).thenReturn(1L);

    // when
    DirectMessageDtoCursorResponse response = directMessageService.getDirectMessagesByUserId(userId, cursorStr, idAfterStr, limit);

    // then
    assertNotNull(response);
    assertEquals(1, response.data().size());
    assertFalse(response.hasNext());
    assertEquals(dmId, response.data().get(0).id());

    verify(userRepository).findById(userId);
    verify(directMessageRepositoryCustom).findDirectMessagesWithCursor(userId, null, null, limit + 1);
    verify(directMessageRepositoryCustom).countDirectMessagesByUserId(userId);
  }

  @Test
  @DisplayName("다이렉트 메시지 조회 실패 - 사용자 없음")
  void getDirectMessageByUserId_UserNotFound() {
    // given
    UUID userId = UUID.randomUUID();

    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // when & then
    assertThrows(RestException.class, () -> directMessageService.getDirectMessagesByUserId(userId, null, null, 5));

    verify(userRepository).findById(userId);
    verifyNoMoreInteractions(directMessageRepositoryCustom);
  }
}