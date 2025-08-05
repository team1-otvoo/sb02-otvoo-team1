package com.team1.otvoo.user.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.team1.otvoo.user.dto.ProfileDto;
import com.team1.otvoo.user.entity.Gender;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.ProfileImage;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.weather.entity.SkyStatus;
import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.entity.WeatherLocation;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProfileMapperTest {

  @Autowired
  private ProfileMapper profileMapper;

  @Test
  void toProfileDto_정상_매핑된다() {
    // given
    WeatherForecast forecast = new WeatherForecast(Instant.now(), Instant.now(), SkyStatus.CLEAR);

    int x = 60;
    int y = 127;
    double latitude = 37.5665;
    double longitude = 126.9780;
    List<String> locationNames = List.of("서울특별시", "중구", "소공동");

    WeatherLocation location = new WeatherLocation(
        forecast,
        x,
        y,
        latitude,
        longitude,
        locationNames
    );

    User user = new User("test@example.com", "encoded-password");
    UUID userId = UUID.randomUUID();
    TestUtils.setField(user, "id", userId);
    TestUtils.setField(user, "createdAt", Instant.now());

    Profile profile = new Profile("test", user);
    TestUtils.setField(profile, "id", UUID.randomUUID());
    TestUtils.setField(profile, "gender", Gender.MALE);
    TestUtils.setField(profile, "birth", LocalDate.of(1990, 1, 1));
    TestUtils.setField(profile, "temperatureSensitivity", 1);
    TestUtils.setField(profile, "location", location);

    ProfileImage profileImage = new ProfileImage(
        "testUrl",
        "originalFilename",
        "contentType",
        10L,
        5,
        5,
        profile
    );

    // when
    ProfileDto dto = profileMapper.toProfileDto(user.getId(), profile, profileImage.getImageUrl());

    // then
    assertThat(dto).isNotNull();
    assertThat(dto.userId()).isEqualTo(user.getId());
    assertThat(dto.name()).isEqualTo("test");
    assertThat(dto.gender()).isEqualTo(Gender.MALE);
    assertThat(dto.birthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
    assertThat(dto.temperatureSensitivity()).isEqualTo(1);
    assertThat(dto.profileImageUrl()).isEqualTo("testUrl");
    assertThat(dto.location().locationNames()).containsExactly("서울특별시", "중구", "소공동");
  }
}
