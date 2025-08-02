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
    ProfileImage profileImage = new ProfileImage(
        imageUrl,
        "originalFilename",
        "contentType",
        10L,
        5,
        5
    );

    Profile profile = new Profile(name);
    profile.updateProfileImage(profileImage);

    User user = new User("test@example.com", "encoded-password", profile);
    TestUtils.setField(user, "id", UUID.randomUUID());
    TestUtils.setField(user, "createdAt", Instant.now());
    return user;
  }

  @Test
  void toUserDto_shouldMapCorrectly() {
    // given
    User user = createMockUser("홍길동", "http://image.url/sample.jpg");

    // when
    UserDto dto = userMapper.toUserDto(user);

    // then
    assertThat(dto.id()).isEqualTo(user.getId());
    assertThat(dto.createdAt()).isEqualTo(user.getCreatedAt());
    assertThat(dto.email()).isEqualTo(user.getEmail());
    assertThat(dto.name()).isEqualTo(user.getProfile().getName());
    assertThat(dto.role()).isEqualTo(user.getRole());
    assertThat(dto.locked()).isEqualTo(user.isLocked());
    assertThat(dto.linkedOAuthProviders()).isEmpty();
  }

  @Test
  void toSummary_shouldMapCorrectly() {
    // given
    User user = createMockUser("김요약", "http://img/summary.png");

    // when
    UserSummary summary = userMapper.toSummary(user);

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
    AuthorDto dto = userMapper.toAuthorDto(user);

    // then
    assertThat(dto.userId()).isEqualTo(user.getId());
    assertThat(dto.name()).isEqualTo("작성자");
    assertThat(dto.profileImageUrl()).isEqualTo("http://img/author.jpg");
  }
}
