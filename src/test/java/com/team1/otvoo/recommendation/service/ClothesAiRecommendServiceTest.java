package com.team1.otvoo.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.otvoo.clothes.dto.OotdDto;
import com.team1.otvoo.clothes.entity.Clothes;
import com.team1.otvoo.clothes.entity.ClothesType;
import com.team1.otvoo.clothes.mapper.ClothesMapper;
import com.team1.otvoo.clothes.repository.ClothesImageRepository;
import com.team1.otvoo.clothes.repository.ClothesRepository;
import com.team1.otvoo.recommendation.client.OpenAiClient;
import com.team1.otvoo.recommendation.dto.FilteredClothesResponse;
import com.team1.otvoo.recommendation.dto.RecommendationDto;
import com.team1.otvoo.recommendation.entity.Recommendation;
import com.team1.otvoo.recommendation.entity.RecommendationClothes;
import com.team1.otvoo.recommendation.repository.ClothesAiAttributesRepository;
import com.team1.otvoo.recommendation.repository.RecommendationRepository;
import com.team1.otvoo.security.CustomUserDetails;
import com.team1.otvoo.storage.S3ImageStorage;
import com.team1.otvoo.user.dto.ProfileDto;
import com.team1.otvoo.user.entity.Gender;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.mapper.ProfileMapper;
import com.team1.otvoo.user.repository.ProfileRepository;
import com.team1.otvoo.weather.dto.WeatherDto;
import com.team1.otvoo.weather.entity.SkyStatus;
import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.mapper.WeatherMapper;
import com.team1.otvoo.weather.repository.WeatherForecastRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClothesAiRecommendServiceTest {

  @InjectMocks
  private ClothesAiRecommendService clothesAiRecommendService;

  @Mock
  private ClothesRepository clothesRepository;
  @Mock
  private ClothesAiAttributesRepository clothesAiAttributesRepository;
  @Mock
  private WeatherForecastRepository weatherForecastRepository;
  @Mock
  private ProfileRepository profileRepository;
  @Mock
  private RecommendationRepository recommendationRepository;
  @Mock
  private ClothesImageRepository clothesImageRepository;
  @Mock
  private S3ImageStorage s3ImageStorage;
  @Mock
  private WeatherMapper weatherMapper;
  @Mock
  private ProfileMapper profileMapper;
  @Mock
  private ClothesMapper clothesMapper;
  @Mock
  private OpenAiClient openAiClient;

  @Mock
  private ObjectMapper objectMapper;

  private User testUser;
  private WeatherForecast testWeather;

  @BeforeEach
  void setUp() {
    // given
    testUser = mock(User.class);
    UUID userId = UUID.randomUUID();
    when(testUser.getId()).thenReturn(userId);

    testWeather = WeatherForecast.of(
        Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS), SkyStatus.CLEAR
    );
    ReflectionTestUtils.setField(testWeather, "id", UUID.randomUUID());

    // SecurityContext에 CustomUserDetails 심기
    CustomUserDetails userDetails = new CustomUserDetails(testUser);
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(userDetails, null, List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @Test
  void filterAndRecommendClothes_success() throws Exception {
    // given
    UUID weatherId = testWeather.getId();
    UUID clothesId = UUID.randomUUID();

    when(objectMapper.writeValueAsString(any()))
        .thenReturn("{\"dummy\":\"json\"}");

    when(weatherForecastRepository.findByIdFetch(weatherId))
        .thenReturn(Optional.of(testWeather));

    Profile profile = mock(Profile.class);
    when(profileRepository.findByUserId(testUser.getId()))
        .thenReturn(Optional.of(profile));

    ProfileDto profileDto = new ProfileDto(
        testUser.getId(),
        "테스트유저",
        Gender.MALE,
        LocalDate.of(1995, 1, 1),
        null,
        3,
        "image.url"
    );
    when(profileMapper.toProfileDto(any(), any(), any())).thenReturn(profileDto);

    // 옷 Mock
    Clothes clothes = mock(Clothes.class);
    when(clothes.getId()).thenReturn(clothesId);
    when(clothes.getName()).thenReturn("셔츠");
    when(clothes.getType()).thenReturn(ClothesType.TOP);
    when(clothes.getSelectedValues()).thenReturn(List.of());

    when(clothesRepository.findByUserIdFetch(testUser.getId()))
        .thenReturn(List.of(clothes));
    when(clothesAiAttributesRepository.findByUserIdClothes_TypeInFetch(testUser.getId()))
        .thenReturn(List.of());

    when(weatherMapper.toDto(testWeather)).thenReturn(mock(WeatherDto.class));

    // LLM 응답 Mock
    FilteredClothesResponse response = new FilteredClothesResponse(List.of(clothesId));
    when(openAiClient.filterClothes(anyString())).thenReturn(response);

    // Recommendation 저장 Mock
    Recommendation recommendation = new Recommendation(testUser, testWeather);
    recommendation.addClothes(new RecommendationClothes(clothes));
    when(recommendationRepository.save(any())).thenReturn(recommendation);

    // OotdDto Mock 변환
    OotdDto ootdDto = OotdDto.builder()
        .clothesId(clothesId)
        .name("셔츠")
        .imageUrl("url")
        .type(ClothesType.TOP)
        .attributes(List.of())
        .build();
    when(clothesMapper.toOotdDto(any(), any())).thenReturn(ootdDto);

    // when
    RecommendationDto result = clothesAiRecommendService.filterAndRecommendClothes(weatherId);

    // then
    assertThat(result.weatherId()).isEqualTo(weatherId);
    assertThat(result.userId()).isEqualTo(testUser.getId());
    assertThat(result.clothes()).isNotNull().hasSize(1);
    assertThat(result.clothes().get(0).getName()).isEqualTo("셔츠");

    verify(openAiClient).filterClothes(anyString());
    verify(recommendationRepository).deleteAllByWeather_Id(weatherId);
    verify(recommendationRepository).save(any(Recommendation.class));
  }
}
