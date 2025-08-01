package com.team1.otvoo.directmessage.service;

import com.team1.otvoo.directmessage.dto.DirectMessageCreateRequest;
import com.team1.otvoo.directmessage.dto.DirectMessageDto;
import com.team1.otvoo.directmessage.dto.DirectMessageDtoCursorResponse;
import com.team1.otvoo.directmessage.dto.DirectMessageResponse;
import com.team1.otvoo.directmessage.entity.DirectMessage;
import com.team1.otvoo.directmessage.mapper.DirectMessageMapper;
import com.team1.otvoo.directmessage.repository.DirectMessageRepository;
import com.team1.otvoo.directmessage.repository.DirectMessageRepositoryCustom;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.user.dto.UserSummary;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

  @Mock
  private DirectMessageMapper directMessageMapper;

  @InjectMocks
  private DirectMessageServiceImpl directMessageService;

  private User sender;
  private User receiver;

  @BeforeEach
  void setup() {
    sender = User.builder()
        .email("sender@test.com")
        .password("password")
        .profile(new Profile("senderName"))
        .build();

    receiver = User.builder()
        .email("receiver@test.com")
        .password("password")
        .profile(new Profile("receiverName"))
        .build();
  }

  @Test
  void create_Success() {
    // given
    UUID senderId = UUID.randomUUID();
    UUID receiverId = UUID.randomUUID();
    String content = "메시지 테스트";
    DirectMessageCreateRequest request = new DirectMessageCreateRequest(senderId, receiverId, content);

    UUID messageId = UUID.randomUUID();

    DirectMessage savedMessage = DirectMessage.builder()
        .id(messageId)
        .sender(sender)
        .receiver(receiver)
        .content(content)
        .createdAt(Instant.now())
        .build();

    DirectMessageResponse expectedResponse = new DirectMessageResponse(senderId, receiverId, content, Instant.now());

    when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
    when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
    when(directMessageRepository.save(any())).thenReturn(savedMessage);
    when(directMessageMapper.toResponse(any(DirectMessage.class))).thenReturn(expectedResponse);

    // when
    DirectMessageResponse actual = directMessageService.create(request);

    // then
    assertEquals(expectedResponse, actual);
    verify(directMessageRepository).save(any());
  }

  @Test
  void create_SenderNotFound() {
    // given
    UUID senderId = UUID.randomUUID();
    UUID receiverId = UUID.randomUUID();
    DirectMessageCreateRequest request = new DirectMessageCreateRequest(senderId, receiverId, "메시지 테스트");

    when(userRepository.findById(senderId)).thenReturn(Optional.empty());

    // when & then
    assertThrows(RestException.class, () -> directMessageService.create(request));
    verify(userRepository, never()).findById(receiverId);
  }

  @Test
  void create_ReceiverNotFound() {
    // given
    UUID senderId = UUID.randomUUID();
    UUID receiverId = UUID.randomUUID();
    DirectMessageCreateRequest request = new DirectMessageCreateRequest(senderId, receiverId, "메시지 테스트");

    when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
    when(userRepository.findById(receiverId)).thenReturn(Optional.empty());

    // when & then
    assertThrows(RestException.class, () -> directMessageService.create(request));
  }

  @Test
  void getDirectMessageByUserId_Success() {
    // given
    UUID userId = UUID.randomUUID();
    String cursorStr = null;
    String idAfterStr = null;
    int limit = 2;

    UUID dmId = UUID.randomUUID();
    Instant createdAt = Instant.now();

    DirectMessage dm = DirectMessage.builder()
        .id(dmId)
        .sender(sender)
        .receiver(receiver)
        .content("테스트 메시지")
        .createdAt(createdAt)
        .build();

    List<DirectMessage> dmList = List.of(dm);

    UserSummary senderSummary = new UserSummary(sender.getId(), sender.getProfile().getName(), null);
    UserSummary receiverSummary = new UserSummary(receiver.getId(), receiver.getProfile().getName(), null);

    DirectMessageDto dmDto = new DirectMessageDto(
        dmId,
        createdAt,
        senderSummary,
        receiverSummary,
        "테스트 메시지"
    );

    when(userRepository.findById(userId)).thenReturn(Optional.of(sender));
    when(directMessageRepositoryCustom.findDirectMessagesWithCursor(userId, null, null, limit + 1)).thenReturn(dmList);
    when(directMessageRepositoryCustom.countDirectMessagesByUserId(userId)).thenReturn(1L);
    when(directMessageMapper.toDto(dm)).thenReturn(dmDto);

    // when
    DirectMessageDtoCursorResponse response = directMessageService.getDirectMessageByuserId(userId, cursorStr, idAfterStr, limit);

    // then
    assertNotNull(response);
    assertEquals(1, response.data().size());
    assertFalse(response.hasNext());
    assertEquals(dmId, response.data().get(0).id());
    verify(userRepository).findById(userId);
    verify(directMessageRepositoryCustom).findDirectMessagesWithCursor(userId, null, null, limit + 1);
    verify(directMessageRepositoryCustom).countDirectMessagesByUserId(userId);
    verify(directMessageMapper).toDto(dm);
  }

  @Test
  void getDirectMessageByuserId_UserNotFound() {
    // given
    UUID userId = UUID.randomUUID();

    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // when & then
    assertThrows(RestException.class, () -> directMessageService.getDirectMessageByuserId(userId, null, null, 5));
    verify(userRepository).findById(userId);
    verifyNoMoreInteractions(directMessageRepositoryCustom, directMessageMapper);
  }
}