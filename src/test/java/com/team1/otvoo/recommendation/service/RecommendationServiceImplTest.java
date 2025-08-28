package com.team1.otvoo.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.Mockito.never;

import com.team1.otvoo.clothes.dto.OotdDto;
import com.team1.otvoo.clothes.entity.Clothes;
import com.team1.otvoo.clothes.entity.ClothesImage;
import com.team1.otvoo.clothes.entity.ClothesType;
import com.team1.otvoo.clothes.mapper.ClothesMapper;
import com.team1.otvoo.clothes.repository.ClothesImageRepository;
import com.team1.otvoo.recommendation.dto.RecommendationDto;
import com.team1.otvoo.recommendation.entity.Recommendation;
import com.team1.otvoo.recommendation.entity.RecommendationClothes;
import com.team1.otvoo.recommendation.repository.RecommendationRepository;
import com.team1.otvoo.security.CustomUserDetails;
import com.team1.otvoo.storage.S3ImageStorage;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.weather.entity.SkyStatus;
import com.team1.otvoo.weather.entity.WeatherForecast;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

  @Mock
  private ClothesAiRecommendService clothesAiRecommendService;
  @Mock
  private RecommendationRepository recommendationRepository;
  @Mock
  private ClothesImageRepository clothesImageRepository;
  @Mock
  private S3ImageStorage s3ImageStorage;
  @Mock
  private ClothesMapper clothesMapper;

  @InjectMocks
  private RecommendationServiceImpl recommendationService;

  @Test
  @DisplayName("추천 정보가 없으면 새로 생성")
  void get_shouldCallAiService_whenNoRecommendationExists() {
    // given
    UUID weatherId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User user = User.builder().build();
    ReflectionTestUtils.setField(user, "id", userId);

    CustomUserDetails principal = new CustomUserDetails(user);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null)
    );

    RecommendationDto expected = new RecommendationDto(weatherId, UUID.randomUUID(), List.of());
    given(recommendationRepository.findByWeather_IdAndUser_Id(weatherId, userId)).willReturn(Optional.empty());
    given(clothesAiRecommendService.filterAndRecommendClothes(weatherId)).willReturn(expected);

    // when
    RecommendationDto result = recommendationService.get(weatherId);

    // then
    assertThat(result).isEqualTo(expected);
    then(clothesAiRecommendService).should(times(1)).filterAndRecommendClothes(weatherId);
    then(recommendationRepository).should(times(1)).findByWeather_IdAndUser_Id(weatherId, userId);
  }

  @Test
  @DisplayName("추천 정보가 있으면 기존 데이터를 반환")
  void get_shouldReturnExistingRecommendation_whenExists() {
    // given
    UUID weatherId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    User user = User.builder().build();
    ReflectionTestUtils.setField(user, "id", userId);

    CustomUserDetails principal = new CustomUserDetails(user);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null)
    );

    Clothes clothes = new Clothes();
    ReflectionTestUtils.setField(clothes, "id", UUID.randomUUID());

    RecommendationClothes rc = new RecommendationClothes(clothes);

    WeatherForecast weather = new WeatherForecast(Instant.now(), Instant.now(), SkyStatus.CLEAR);
    ReflectionTestUtils.setField(weather, "id", weatherId);
    ;

    Recommendation recommendation = new Recommendation(user,weather);
    recommendation.addClothes(rc);

    given(recommendationRepository.findByWeather_IdAndUser_Id(weatherId, userId)).willReturn(Optional.of(recommendation));

    ClothesImage image = new ClothesImage("key", "file", "image/png", 5L, 5, 5, clothes);
    given(clothesImageRepository.findByClothes_Id(clothes.getId())).willReturn(Optional.of(image));
    given(s3ImageStorage.getPresignedUrl("key", "image/png")).willReturn("http://presigned-url");
    given(clothesMapper.toOotdDto(clothes, "http://presigned-url"))
        .willReturn(new OotdDto(clothes.getId(), "clothes", "http://presigned-url", ClothesType.TOP, List.of()));

    // when
    RecommendationDto result = recommendationService.get(weatherId);

    // then
    assertThat(result.weatherId()).isEqualTo(weatherId);
    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.clothes().size()).isEqualTo(1);

    then(clothesAiRecommendService).should(never()).filterAndRecommendClothes(any());
    then(recommendationRepository).should(times(1)).findByWeather_IdAndUser_Id(weatherId, userId);
    then(clothesImageRepository).should(times(1)).findByClothes_Id(clothes.getId());
    then(s3ImageStorage).should(times(1)).getPresignedUrl("key", "image/png");
    then(clothesMapper).should(times(1)).toOotdDto(clothes, "http://presigned-url");
  }
}
