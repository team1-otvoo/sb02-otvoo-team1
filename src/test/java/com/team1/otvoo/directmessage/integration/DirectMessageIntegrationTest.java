package com.team1.otvoo.directmessage.integration;

import com.team1.otvoo.directmessage.controller.DirectMessageController;
import com.team1.otvoo.directmessage.dto.DirectMessageCreateRequest;
import com.team1.otvoo.directmessage.dto.DirectMessageDtoCursorResponse;
import com.team1.otvoo.directmessage.entity.DirectMessage;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import com.team1.otvoo.directmessage.repository.DirectMessageRepository;
import com.team1.otvoo.directmessage.service.DirectMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class DirectMessageIntegrationTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private DirectMessageRepository directMessageRepository;

  @Autowired
  private DirectMessageService directMessageService;

  @Autowired
  private DirectMessageController directMessageController;

  private User sender;
  private User receiver;

  @BeforeEach
  void setup() {
    sender = User.builder()
        .email("sender@example.com")
        .password("password")
        .profile(new Profile("sender"))
        .build();

    receiver = User.builder()
        .email("receiver@example.com")
        .password("password")
        .profile(new Profile("receiver"))
        .build();

    userRepository.save(sender);
    userRepository.save(receiver);
  }

  @Test
  void testCreateAndFetchDirectMessages() {
    // 1. 메시지 생성
    DirectMessageCreateRequest createRequest = new DirectMessageCreateRequest(
        sender.getId(),
        receiver.getId(),
        "안녕하세요!"
    );

    var response = directMessageService.create(createRequest);

    assertThat(response).isNotNull();
    assertThat(response.senderId()).isEqualTo(sender.getId());
    assertThat(response.receiverId()).isEqualTo(receiver.getId());
    assertThat(response.content()).isEqualTo("안녕하세요!");

    // 2. DB에 메시지 저장 확인
    List<DirectMessage> messages = directMessageRepository.findAll();
    assertThat(messages).isNotEmpty();

    DirectMessage savedMessage = messages.get(0);
    assertThat(savedMessage.getContent()).isEqualTo("안녕하세요!");
    assertThat(savedMessage.getSender().getId()).isEqualTo(sender.getId());
    assertThat(savedMessage.getReceiver().getId()).isEqualTo(receiver.getId());

    // 3. 컨트롤러로 메시지 조회
    ResponseEntity<DirectMessageDtoCursorResponse> entityResponse =
        directMessageController.getDirectMessages(sender.getId(), null, null, 10);

    assertThat(entityResponse.getStatusCodeValue()).isEqualTo(200);
    DirectMessageDtoCursorResponse cursorResponse = entityResponse.getBody();
    assertThat(cursorResponse).isNotNull();
    assertThat(cursorResponse.data()).isNotEmpty();

    var firstMessage = cursorResponse.data().get(0);
    assertThat(firstMessage.content()).isEqualTo("안녕하세요!");
    assertThat(firstMessage.sender().userId()).isEqualTo(sender.getId());
    assertThat(firstMessage.receiver().userId()).isEqualTo(receiver.getId());
  }
}
