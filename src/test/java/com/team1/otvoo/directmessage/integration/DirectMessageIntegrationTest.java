package com.team1.otvoo.directmessage.integration;

import com.team1.otvoo.directmessage.controller.DirectMessageController;
import com.team1.otvoo.directmessage.dto.DirectMessageCreateRequest;
import com.team1.otvoo.directmessage.dto.DirectMessageDtoCursorResponse;
import com.team1.otvoo.directmessage.entity.DirectMessage;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import com.team1.otvoo.directmessage.repository.DirectMessageRepository;
import com.team1.otvoo.directmessage.service.DirectMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
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
        .build();
    sender = userRepository.save(sender);

    receiver = User.builder()
        .email("receiver@example.com")
        .password("password")
        .build();
    receiver = userRepository.save(receiver);
  }

  @Test
  @DisplayName("다이렉트 메시지 생성 및 조회 성공")
  void testCreateAndFetchDirectMessages() {
    // 1. 메시지 생성 요청 DTO 준비
    DirectMessageCreateRequest createRequest = new DirectMessageCreateRequest(
        sender.getId(),
        receiver.getId(),
        "안녕하세요!"
    );

    // 2. 서비스에서 메시지 생성 및 DTO 반환
    var response = directMessageService.createDto(createRequest);

    // 3. 생성된 메시지 DTO 검증
    assertThat(response).isNotNull();
    assertThat(response.sender().userId()).isEqualTo(sender.getId());
    assertThat(response.receiver().userId()).isEqualTo(receiver.getId());
    assertThat(response.content()).isEqualTo("안녕하세요!");

    // 4. DB에 메시지 저장 여부 확인
    List<DirectMessage> messages = directMessageRepository.findAll();
    assertThat(messages).isNotEmpty();

    DirectMessage savedMessage = messages.get(0);
    assertThat(savedMessage.getContent()).isEqualTo("안녕하세요!");
    assertThat(savedMessage.getSender().getId()).isEqualTo(sender.getId());
    assertThat(savedMessage.getReceiver().getId()).isEqualTo(receiver.getId());

    // 5. 컨트롤러를 통한 메시지 조회 API 호출
    ResponseEntity<DirectMessageDtoCursorResponse> entityResponse =
        directMessageController.getDirectMessages(sender.getId(), null, null, 10);

    // 6. API 응답 검증
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