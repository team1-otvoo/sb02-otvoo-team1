package com.team1.otvoo.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.Mockito.never;

import com.team1.otvoo.clothes.entity.Clothes;
import com.team1.otvoo.recommendation.client.OpenAiClient;
import com.team1.otvoo.recommendation.dto.VisionAttributeResponseDto;
import com.team1.otvoo.recommendation.entity.ClothesAiAttributes;
import com.team1.otvoo.recommendation.repository.ClothesAiAttributesRepository;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClothesAiAttributeServiceTest {

  @Mock
  private ClothesAiAttributesRepository clothesAiAttributesRepository;
  @Mock
  private OpenAiClient openAiClient;
  @InjectMocks
  private ClothesAiAttributeService clothesAiAttributeService;

  @Test
  @DisplayName("의상 속성 추출 성공")
  void extractAndSaveAttributes_success() {
    // given
    Clothes clothes = new Clothes();
    String imageUrl = "http://example.com/image.jpg";

    VisionAttributeResponseDto dto = new VisionAttributeResponseDto(Map.of("color", "blue"));
    given(openAiClient.analyzeImage(imageUrl)).willReturn(dto);

    ClothesAiAttributes savedEntity = new ClothesAiAttributes(clothes, dto.attributes());
    given(clothesAiAttributesRepository.save(any())).willReturn(savedEntity);

    // when
    ClothesAiAttributes result = clothesAiAttributeService.extractAndSaveAttributes(clothes, imageUrl);

    // then
    assertThat(result.getAttributes()).isEqualTo(Map.of("color", "blue"));
    then(openAiClient).should(times(1)).analyzeImage(imageUrl);
    then(clothesAiAttributesRepository).should(times(1)).save(any());
  }

  @Test
  @DisplayName("의상 속성 추출 - 이미지가 없을 때 null 반환")
  void extractAndSaveAttributes_shouldReturnNull_whenImageUrlIsNull() {
    // given
    Clothes clothes = new Clothes();

    // when
    ClothesAiAttributes result = clothesAiAttributeService.extractAndSaveAttributes(clothes, null);

    // then
    assertThat(result).isNull();
    then(openAiClient).should(never()).analyzeImage(any());
    then(clothesAiAttributesRepository).should(never()).save(any());
  }
}