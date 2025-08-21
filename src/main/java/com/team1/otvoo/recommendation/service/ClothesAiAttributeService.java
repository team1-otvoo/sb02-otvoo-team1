package com.team1.otvoo.recommendation.service;

import com.team1.otvoo.clothes.entity.Clothes;
import com.team1.otvoo.recommendation.client.OpenAiClient;
import com.team1.otvoo.recommendation.dto.VisionAttributeResponseDto;
import com.team1.otvoo.recommendation.entity.ClothesAiAttributes;
import com.team1.otvoo.recommendation.repository.ClothesAiAttributesRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesAiAttributeService {

  private final ClothesAiAttributesRepository clothesAiAttributesRepository;
  private final OpenAiClient openAiClient;

  // 의상 이미지 기반 Vision 속성 추출 및 저장
  @Transactional
  public ClothesAiAttributes extractAndSaveAttributes(Clothes clothes, String imageUrl) {
    // 1. LLM Vision API 호출
    VisionAttributeResponseDto response = openAiClient.analyzeImage(imageUrl);

    log.info("Vision API 추출 결과: {}", response.attributes());

    // 2. 새 엔티티 생성 후 저장
    ClothesAiAttributes entity = new ClothesAiAttributes(clothes, response.attributes());
    return clothesAiAttributesRepository.save(entity);
  }

  // 속성 조회 -> 추천 로직 등에서 사용
  @Transactional(readOnly = true)
  public Optional<ClothesAiAttributes> getAttributes(UUID clothesId) {
    return clothesAiAttributesRepository.findByClothesId(clothesId);
  }
}
