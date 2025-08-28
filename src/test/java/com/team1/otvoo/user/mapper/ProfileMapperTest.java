package com.team1.otvoo.user.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.team1.otvoo.user.dto.ProfileDto;
import com.team1.otvoo.user.entity.Gender;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.ProfileImage;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.weather.entity.WeatherLocation;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.team1.otvoo.weather.mapper.WeatherMapper; // WeatherMapper import


@ExtendWith(MockitoExtension.class) // Mockito 기능을 JUnit5에서 사용
class ProfileMapperTest {

  // @InjectMocks: 이 객체를 생성하고, @Spy 또는 @Mock으로 표시된 필드를 주입합니다.
  @InjectMocks
  private ProfileMapper profileMapper = Mappers.getMapper(ProfileMapper.class);

  // @Spy: 실제 객체를 사용하지만, Mockito가 제어할 수 있게 래핑
  @Spy
  private WeatherMapper weatherMapper = Mappers.getMapper(WeatherMapper.class);

  @Test
  @DisplayName("사용자 ID, 프로필, 이미지 URL을 받아 ProfileDto로 정상 매핑한다")
  void toProfileDto_정상_매핑된다() {
    // given, when, then 코드는 기존과 완전히 동일합니다.
    // ... (기존 테스트 로직)

    // given
    List<String> locationNames = List.of("서울특별시", "중구", "소공동");
    WeatherLocation location = new WeatherLocation(60, 127, 37.5665, 126.9780, locationNames);
    User user = new User("test@example.com", "encoded-password");
    TestUtils.setField(user, "id", UUID.randomUUID());

    Profile profile = new Profile("test", user);
    TestUtils.setField(profile, "gender", Gender.MALE);
    TestUtils.setField(profile, "birth", LocalDate.of(1990, 1, 1));
    TestUtils.setField(profile, "temperatureSensitivity", 1);
    TestUtils.setField(profile, "location", location);

    ProfileImage profileImage = new ProfileImage("testUrl", "original", "image/png", 10L, 5, 5, profile);

    // when
    ProfileDto dto = profileMapper.toProfileDto(user.getId(), profile, profileImage.getObjectKey());

    // then
    assertThat(dto).isNotNull();
    assertThat(dto.userId()).isEqualTo(user.getId());
    assertThat(dto.name()).isEqualTo("test");
    assertThat(dto.gender()).isEqualTo(Gender.MALE);
    assertThat(dto.location().locationNames()).containsExactly("서울특별시", "중구", "소공동");
  }
}