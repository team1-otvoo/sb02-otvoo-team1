package com.team1.otvoo.user.mapper;

import com.team1.otvoo.user.dto.AuthorDto;
import com.team1.otvoo.user.dto.UserDto;
import com.team1.otvoo.user.dto.UserSummary;
import com.team1.otvoo.user.entity.*;
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
}
