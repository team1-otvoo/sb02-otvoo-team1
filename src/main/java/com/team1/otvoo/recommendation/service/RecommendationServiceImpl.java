package com.team1.otvoo.recommendation.service;

import com.team1.otvoo.clothes.dto.OotdDto;
import com.team1.otvoo.clothes.entity.Clothes;
import com.team1.otvoo.clothes.entity.ClothesImage;
import com.team1.otvoo.clothes.mapper.ClothesMapper;
import com.team1.otvoo.clothes.repository.ClothesImageRepository;
import com.team1.otvoo.recommendation.dto.RecommendationDto;
import com.team1.otvoo.recommendation.entity.Recommendation;
import com.team1.otvoo.recommendation.repository.RecommendationRepository;
import com.team1.otvoo.storage.S3ImageStorage;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {
  private final ClothesAiRecommendService clothesAiRecommendService;
  private final RecommendationRepository recommendationRepository;
  private final ClothesImageRepository clothesImageRepository;
  private final S3ImageStorage s3ImageStorage;
  private final ClothesMapper clothesMapper;

  @Override
  public RecommendationDto refresh(UUID weatherId) {
    return clothesAiRecommendService.filterAndRecommendClothes(weatherId);
  }

  @Override
  public RecommendationDto get(UUID weatherId) {
    Recommendation recommendation = recommendationRepository.findByWeather_Id(weatherId).orElse(null);
    RecommendationDto recommendationDto = null;

    // 추천 정보가 없다면 새로 생성
    if(recommendation == null) {
      recommendationDto = clothesAiRecommendService.filterAndRecommendClothes(weatherId);
    } else {
      // 있다면 기존에 있는 추천 정보 사용
      List<OotdDto> ootds = recommendation.getClothes().stream()
          .map(rc -> {
            Clothes clothes = rc.getClothes();
            ClothesImage image = clothesImageRepository.findByClothes_Id(clothes.getId())
                .orElse(null);
            String url = (image != null)
                ? s3ImageStorage.getPresignedUrl(image.getImageKey(), image.getContentType())
                : null;
            return clothesMapper.toOotdDto(clothes, url);
          })
          .toList();

      recommendationDto = new RecommendationDto(recommendation.getWeather().getId(),
          recommendation.getUser().getId(),
          ootds);
    }

    return recommendationDto;
  }
}
