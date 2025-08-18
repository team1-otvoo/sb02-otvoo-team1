package com.team1.otvoo.user.mapper;

import com.team1.otvoo.user.dto.AuthorDto;
import com.team1.otvoo.user.dto.UserDto;
import com.team1.otvoo.user.dto.UserRow;
import com.team1.otvoo.user.dto.UserSummary;
import com.team1.otvoo.user.entity.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

  private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

  private User createMockUser(String name, String imageUrl) {

    User user = new User("test@example.com", "encoded-password");
    TestUtils.setField(user, "id", UUID.randomUUID());
    TestUtils.setField(user, "createdAt", Instant.now());

    Profile profile = new Profile(name, user);

    ProfileImage profileImage = new ProfileImage(
        imageUrl,
        "originalFilename",
        "contentType",
        10L,
        5,
        5,
        profile
    );

    return user;
  }

  @Test
  void toUserDto_shouldMapCorrectly() {
    // given
    String name = "홍길동";
    User user = createMockUser(name, "http://image.url/sample.jpg");

    // when
    UserDto dto = userMapper.toUserDto(user, name);

    // then
    assertThat(dto.id()).isEqualTo(user.getId());
    assertThat(dto.createdAt()).isEqualTo(user.getCreatedAt());
    assertThat(dto.email()).isEqualTo(user.getEmail());
    assertThat(dto.name()).isEqualTo(name);
    assertThat(dto.role()).isEqualTo(user.getRole());
    assertThat(dto.locked()).isEqualTo(user.isLocked());
    assertThat(dto.linkedOAuthProviders()).isEmpty();
  }

  @Test
  void toSummary_shouldMapCorrectly() {
    // given
    User user = createMockUser("김요약", "http://img/summary.png");

    // when
    UserSummary summary = userMapper.toSummary(user, "김요약", "http://img/summary.png");

    // then
    assertThat(summary.userId()).isEqualTo(user.getId());
    assertThat(summary.name()).isEqualTo("김요약");
    assertThat(summary.profileImageUrl()).isEqualTo("http://img/summary.png");
  }

  @Test
  void toAuthorDto_shouldMapCorrectly() {
    // given
    User user = createMockUser("작성자", "http://img/author.jpg");

    // when
    AuthorDto dto = userMapper.toAuthorDto(user, "작성자", "http://img/author.jpg");

    // then
    assertThat(dto.userId()).isEqualTo(user.getId());
    assertThat(dto.name()).isEqualTo("작성자");
    assertThat(dto.profileImageUrl()).isEqualTo("http://img/author.jpg");
  }

  @Test
  void toUserDtoFromUserRow_shouldMapCorrectly() {
    // given
    UUID id = UUID.randomUUID();
    Instant createdAt = Instant.parse("2024-12-31T23:59:59Z");
    String email = "rowuser@example.com";
    String name = "로우유저";
    Role role = Role.USER;
    boolean locked = true;

    // ⚠️ 프로젝트의 UserRow 정의에 맞게 생성자를 확인하세요.
    // 예: record UserRow(UUID id, Instant createdAt, String email, String name, Role role, boolean locked)
    UserRow row = new UserRow(id, createdAt, email, name, role, locked);

    // OAuth 제공자 목록(매핑에서 그대로 복사되어야 함)
    List<String> providers = List.of("google", "github");

    // when
    UserDto dto = userMapper.toUserDtoFromUserRow(row, providers);

    // then
    assertThat(dto.id()).isEqualTo(id);
    assertThat(dto.createdAt()).isEqualTo(createdAt);
    assertThat(dto.email()).isEqualTo(email);
    assertThat(dto.name()).isEqualTo(name);
    assertThat(dto.role()).isEqualTo(role);
    assertThat(dto.locked()).isEqualTo(locked);
    assertThat(dto.linkedOAuthProviders()).containsExactlyElementsOf(providers);
  }
}
