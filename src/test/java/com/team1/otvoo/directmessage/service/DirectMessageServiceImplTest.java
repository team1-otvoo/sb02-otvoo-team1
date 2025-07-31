package com.team1.otvoo.directmessage.service;

import com.team1.otvoo.directmessage.dto.DirectMessageCreateRequest;
import com.team1.otvoo.directmessage.dto.DirectMessageResponse;
import com.team1.otvoo.directmessage.entity.DirectMessage;
import com.team1.otvoo.directmessage.mapper.DirectMessageMapper;
import com.team1.otvoo.directmessage.repository.DirectMessageRepository;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
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
  private DirectMessageMapper directMessageMapper;

  @InjectMocks
  private DirectMessageServiceImpl directMessageService;

  @Test
  void create_Success() {
    // given
    UUID senderId = UUID.randomUUID();
    UUID receiverId = UUID.randomUUID();
    String content = "메시지 테스트";
    DirectMessageCreateRequest request = new DirectMessageCreateRequest(senderId, receiverId, content);

    User sender = User.builder()
        .email("sender@test.com")
        .password("password")
        .profile(new Profile("senderProfile"))
        .build();

    User receiver = User.builder()
        .email("receiver@test.com")
        .password("password")
        .profile(new Profile("receiverProfile"))
        .build();

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

    User sender = User.builder()
        .email("sender@test.com")
        .password("password")
        .profile(new Profile("senderProfile"))
        .build();

    when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
    when(userRepository.findById(receiverId)).thenReturn(Optional.empty());

    // when & then
    assertThrows(RestException.class, () -> directMessageService.create(request));
  }
}
