package com.team1.otvoo.clothes.service;

import static org.mockito.BDDMockito.*;
import static org.assertj.core.api.Assertions.*;

import com.team1.otvoo.clothes.dto.ClothesAttributeDefCreateRequest;
import com.team1.otvoo.clothes.dto.ClothesAttributeDefDto;
import com.team1.otvoo.clothes.entity.ClothesAttributeDefinition;
import com.team1.otvoo.clothes.entity.ClothesAttributeValue;
import com.team1.otvoo.clothes.mapper.ClothesAttributeDefMapper;
import com.team1.otvoo.clothes.repository.ClothesAttributeDefRepository;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClothesAttributeDefServiceImplTest {

  @Mock
  private ClothesAttributeDefRepository clothesAttributeDefRepository;
  @Mock
  private ClothesAttributeDefMapper clothesAttributeDefMapper;

  @InjectMocks
  private ClothesAttributeDefServiceImpl clothesAttributeDefService;

  private ClothesAttributeDefinition clothesAttributeDefinition;
  private ClothesAttributeDefDto clothesAttributeDefDto;
  private String name;
  private List<String> selectableValues;

  @BeforeEach
  void setUp() {
    name = "색상";
    selectableValues = List.of("빨강", "파랑", "초록");

    List<ClothesAttributeValue> values = selectableValues.stream()
        .map(ClothesAttributeValue::new)
        .toList();

    clothesAttributeDefinition = new ClothesAttributeDefinition(name, values);

    clothesAttributeDefDto = new ClothesAttributeDefDto(
        UUID.randomUUID(),
        name,
        selectableValues
    );
  }

  @Test
  @DisplayName("의상 속성 등록 성공")
  void createClothesAttributeDef_Success() {
    // given
    ClothesAttributeDefCreateRequest request = new ClothesAttributeDefCreateRequest(name,
        selectableValues);

    when(clothesAttributeDefRepository.existsByName(name)).thenReturn(false);
    when(clothesAttributeDefRepository.save(any(ClothesAttributeDefinition.class)))
        .thenReturn(clothesAttributeDefinition);
    when(clothesAttributeDefMapper.toDto(clothesAttributeDefinition))
        .thenReturn(clothesAttributeDefDto);

    // when
    ClothesAttributeDefDto result = clothesAttributeDefService.create(request);

    // then
    assertThat(result).isNotNull();
    assertThat(result.name()).isEqualTo(name);
    assertThat(result.selectableValues()).containsExactlyElementsOf(selectableValues);
  }

  @Test
  @DisplayName("속성 등록 실패_중복된 속성 이름")
  void createClothesAttributeDef_DuplicateName() {
    // given
    ClothesAttributeDefCreateRequest request = new ClothesAttributeDefCreateRequest(name,
        selectableValues);

    // 이미 속성 이름 존재
    when(clothesAttributeDefRepository.existsByName(name)).thenReturn(true);

    // when & then
    assertThatThrownBy(() -> clothesAttributeDefService.create(request))
        .isInstanceOf(RestException.class)
        .hasMessage(ErrorCode.ATTRIBUTE_DEFINITION_DUPLICATE.getMessage());

    then(clothesAttributeDefRepository).should(never()).save(any());
  }
}