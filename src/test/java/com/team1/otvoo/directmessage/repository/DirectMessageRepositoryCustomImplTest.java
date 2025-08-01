package com.team1.otvoo.directmessage.repository;

import com.team1.otvoo.directmessage.entity.DirectMessage;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

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
        .profile(new Profile("userProfile"))
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
  void testFindDirectMessagesWithCursor() {
    // when
    List<DirectMessage> messages = directMessageRepositoryCustom.findDirectMessagesWithCursor(testUserId, null, null, 10);

    // then
    assertThat(messages).isNotEmpty();
    assertThat(messages.size()).isLessThanOrEqualTo(10);
  }

  @Test
  void testCountDirectMessagesByUserId() {
    // when
    long count = directMessageRepositoryCustom.countDirectMessagesByUserId(testUserId);

    // then
    assertThat(count).isGreaterThan(0);
  }
}