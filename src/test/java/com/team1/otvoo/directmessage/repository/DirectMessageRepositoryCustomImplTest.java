package com.team1.otvoo.directmessage.repository;

import com.team1.otvoo.directmessage.dto.DirectMessageDto;
import com.team1.otvoo.directmessage.entity.DirectMessage;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class DirectMessageRepositoryCustomImplTest {

  @Autowired
  private DirectMessageRepositoryCustom directMessageRepositoryCustom;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private DirectMessageRepository directMessageRepository;

  private UUID testUserId;

  @BeforeEach
  void setUp() {
    User user = User.builder()
        .email("user@test.com")
        .password("password")
        .build();

    userRepository.save(user);
    testUserId = user.getId();

    DirectMessage dm1 = DirectMessage.builder()
        .sender(user)
        .receiver(user)
        .content("첫번째 테스트 메시지")
        .createdAt(Instant.now().minusSeconds(60))
        .build();
    directMessageRepository.save(dm1);

    DirectMessage dm2 = DirectMessage.builder()
        .sender(user)
        .receiver(user)
        .content("두번째 테스트 메시지")
        .createdAt(Instant.now())
        .build();
    directMessageRepository.save(dm2);
  }

  @Test
  @DisplayName("커서 기반 다이렉트 메시지 조회 Between Two Users")
  void testFindDirectMessagesBetweenUsersWithCursor() {
    // given
    UUID userId1 = testUserId;
    UUID userId2 = testUserId;

    Instant cursor = null;
    UUID idAfter = null;
    int limit = 10;

    // when
    List<DirectMessageDto> messages = directMessageRepositoryCustom.findDirectMessagesBetweenUsersWithCursor(userId1, userId2, cursor, idAfter, limit);

    // then
    assertThat(messages).isNotEmpty();
    assertThat(messages.size()).isLessThanOrEqualTo(limit);
    assertThat(messages.get(0).content()).isNotNull();
  }

  @Test
  @DisplayName("사용자 간 다이렉트 메시지 총 개수 조회")
  void testCountDirectMessagesBetweenUsers() {
    // given
    UUID userId1 = testUserId;
    UUID userId2 = testUserId;

    // when
    long count = directMessageRepositoryCustom.countDirectMessagesBetweenUsers(userId1, userId2);

    // then
    assertThat(count).isGreaterThan(0);
  }

  @Test
  @DisplayName("ID로 다이렉트 메시지 및 사용자 정보 조회")
  void testFindByIdWithUserSummaries() {
    // given
    DirectMessage dm = directMessageRepository.findAll().get(0);
    UUID dmId = dm.getId();

    // when
    DirectMessageDto dto = directMessageRepositoryCustom.findByIdWithUserSummaries(dmId);

    // then
    assertThat(dto).isNotNull();
    assertThat(dto.id()).isEqualTo(dmId);
    assertThat(dto.sender()).isNotNull();
    assertThat(dto.receiver()).isNotNull();
    assertThat(dto.content()).isEqualTo(dm.getContent());
  }
}