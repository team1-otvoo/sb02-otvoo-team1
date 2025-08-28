package com.team1.otvoo.recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.otvoo.clothes.dto.OotdDto;
import com.team1.otvoo.clothes.entity.Clothes;
import com.team1.otvoo.clothes.entity.ClothesImage;
import com.team1.otvoo.clothes.mapper.ClothesMapper;
import com.team1.otvoo.clothes.repository.ClothesImageRepository;
import com.team1.otvoo.clothes.repository.ClothesRepository;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.recommendation.client.OpenAiClient;
import com.team1.otvoo.recommendation.dto.ClothesAiDto;
import com.team1.otvoo.recommendation.dto.ClothesFilterWrapperDto;
import com.team1.otvoo.recommendation.dto.FilteredClothesResponse;
import com.team1.otvoo.recommendation.dto.RecommendationDto;
import com.team1.otvoo.recommendation.entity.ClothesAiAttributes;
import com.team1.otvoo.recommendation.entity.Recommendation;
import com.team1.otvoo.recommendation.entity.RecommendationClothes;
import com.team1.otvoo.recommendation.repository.ClothesAiAttributesRepository;
import com.team1.otvoo.recommendation.repository.RecommendationRepository;
import com.team1.otvoo.security.CustomUserDetails;
import com.team1.otvoo.storage.S3ImageStorage;
import com.team1.otvoo.user.dto.ProfileDto;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.mapper.ProfileMapper;
import com.team1.otvoo.user.repository.ProfileRepository;
import com.team1.otvoo.weather.dto.WeatherDto;
import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.mapper.WeatherMapper;
import com.team1.otvoo.weather.repository.WeatherForecastRepository;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClothesAiRecommendService {
  private final ClothesRepository clothesRepository;
  private final ClothesAiAttributesRepository clothesAiAttributesRepository;
  private final WeatherForecastRepository weatherForecastRepository;
  private final ProfileRepository profileRepository;
  private final RecommendationRepository recommendationRepository;
  private final ClothesImageRepository clothesImageRepository;
  private final S3ImageStorage s3ImageStorage;
  private final WeatherMapper weatherMapper;
  private final ProfileMapper profileMapper;
  private final ClothesMapper clothesMapper;
  private final ObjectMapper objectMapper;
  private final OpenAiClient openAiClient;

  @Transactional
  public RecommendationDto filterAndRecommendClothes(UUID weatherId) {
    WeatherForecast weatherForecast = weatherForecastRepository.findByIdFetch(weatherId)
        .orElseThrow(() -> new RestException(ErrorCode.WEATHER_FORECAST_NOT_FOUND,
            Map.of("weatherId", weatherId)));

    User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser();
    UUID userId = user.getId();

    Profile profile = profileRepository.findByUserId(userId).orElseThrow(
        () -> new RestException(ErrorCode.PROFILE_NOT_FOUND, Map.of("userId", userId))
    );

    ProfileDto profileDto = profileMapper.toProfileDto(userId, profile, "image.url");

    List<Clothes> clothesList = clothesRepository.findByUserIdFetch(userId);
    List<ClothesAiAttributes> clothesAiAttributesList =
        clothesAiAttributesRepository.findByUserIdClothes_TypeInFetch(userId);

    // 2-1. 날씨 Entity -> WeatherDto 변환
    WeatherDto weatherDto = weatherMapper.toDto(weatherForecast);

    // 2-2. AI 속성을 빠르게 찾기 위해 Map으로 변환
    Map<UUID, Map<String, String>> aiAttributesMap = clothesAiAttributesList.stream()
        .collect(Collectors.toMap(
            aiAttr -> aiAttr.getClothes().getId(),
            ClothesAiAttributes::getAttributes
        ));

    // 2-3. 옷 Entity List -> List<ClothesAiDto> 변환
    List<ClothesAiDto> clothesAiDtos = clothesList.stream().map(clothes -> {
      Map<String, String> combinedAttributes = new HashMap<>();
      // 사용자 선택 속성 추가
      clothes.getSelectedValues().forEach(sv ->
          combinedAttributes.put(sv.getDefinition().getName(), sv.getValue().getValue())
      );
      // AI 추출 속성 추가
      Map<String, String> aiAttrs = aiAttributesMap.get(clothes.getId());
      if (aiAttrs != null) {
        combinedAttributes.putAll(aiAttrs);
      }
      return new ClothesAiDto(clothes.getId(), clothes.getName(), clothes.getType(),
          combinedAttributes);
    }).toList();

    // 2-4. 최종 요청 DTO 생성
    ClothesFilterWrapperDto requestPayload = new ClothesFilterWrapperDto(profileDto, weatherDto,
        clothesAiDtos);

    // 2-5. DTO를 JSON 문자열로 변환
    String jsonPayload;
    try {
      jsonPayload = objectMapper.writeValueAsString(requestPayload);

    } catch (JsonProcessingException e) {
      log.error("LLM 요청 DTO를 JSON으로 변환하는 중 에러 발생", e);
      throw new RestException(ErrorCode.JSON_PARSE_ERROR);
    }

    // --- 3. LLM 호출 및 결과 처리 ---
    FilteredClothesResponse responseDto = null;
    try {
      // 3-1. OpenAiClient 호출하여 DTO를 직접 받음
      responseDto = openAiClient.filterClothes(jsonPayload);
    } catch (Exception e) {
      log.error("LLM 호출 또는 응답 처리 중 에러 발생", e);
      throw new RestException(ErrorCode.LLM_PROCESSING_ERROR);
    }

    // 3-2. 기존 저장된 추천 정보 삭제
    recommendationRepository.deleteAllByWeather_Id(weatherId);
    recommendationRepository.flush();

    // 4. 추천 결과 DB에 저장
    Recommendation savedRecommendation = saveRecommendation(user, weatherForecast, responseDto);

    List<OotdDto> ootds = savedRecommendation.getClothes().stream()
        .map(rc -> {
          Clothes clothes = rc.getClothes();
          ClothesImage image = clothesImageRepository.findByClothes_Id(clothes.getId()).orElse(null);
          String url = (image != null)
              ? s3ImageStorage.getPresignedUrl(image.getImageKey(), image.getContentType())
              : null;
          return clothesMapper.toOotdDto(clothes, url);
        })
        .toList();


    return new RecommendationDto(savedRecommendation.getWeather().getId(),
        savedRecommendation.getUser().getId(),
        ootds);
  }

  // 추천 결과를 저장하는 헬퍼 메서드
  private Recommendation saveRecommendation(User user, WeatherForecast weatherForecast,
      FilteredClothesResponse response) {

    // 1. Recommendation 엔티티 생성
    Recommendation recommendation = new Recommendation(user, weatherForecast);

    // 2. 추천받은 옷의 ID로 Clothes 엔티티 조회
    List<UUID> recommendedClothesIds = response.clothesIds();
    List<Clothes> recommendedClothesList = clothesRepository.findAllById(recommendedClothesIds);

    //  중복 제거: 같은 Type이 여러 개 있으면 첫 번째만 남기고 나머지는 제거
    Map<String, Clothes> uniqueByType = new LinkedHashMap<>();
    for (Clothes clothes : recommendedClothesList) {
      String type = clothes.getType().name();
      if (!uniqueByType.containsKey(type)) {
        uniqueByType.put(type, clothes); // 첫 등장한 Type만 저장
      } else {
        log.warn("중복된 Type 발견: {} → 첫 번째만 유지", type);
      }
    }

    // 3. 조회된 Clothes 엔티티를 RecommendationClothes에 추가
    for (Clothes clothes : uniqueByType.values()) {
      RecommendationClothes recommendationClothes = new RecommendationClothes(clothes);
      recommendation.addClothes(recommendationClothes);
    }

    Recommendation savedRecommendation = recommendationRepository.save(recommendation);
    log.info("새로운 추천 저장완료. Recommendation ID: {}", recommendation.getId());

    return savedRecommendation;
  }
}
