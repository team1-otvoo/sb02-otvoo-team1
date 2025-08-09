package com.team1.otvoo.clothes.service;

import static org.mockito.BDDMockito.*;
import static org.assertj.core.api.Assertions.*;

import com.team1.otvoo.clothes.dto.SortBy;
import com.team1.otvoo.clothes.dto.SortDirection;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDefCreateRequest;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDefDto;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDefDtoCursorResponse;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDefSearchCondition;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDefUpdateRequest;
import com.team1.otvoo.clothes.entity.ClothesAttributeDefinition;
import com.team1.otvoo.clothes.entity.ClothesAttributeValue;
import com.team1.otvoo.clothes.mapper.ClothesAttributeDefMapper;
import com.team1.otvoo.clothes.repository.ClothesAttributeDefRepository;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
  private UUID definitionId;
  private String name;
  private List<String> selectableValues;
  private static final Instant CREATED_AT = Instant.parse("2025-01-01T00:00:00Z");

  @BeforeEach
  void setUp() {
    definitionId = UUID.randomUUID();
    name = "색상";
    selectableValues = List.of("빨강", "파랑", "초록");

    List<ClothesAttributeValue> values = selectableValues.stream()
        .map(ClothesAttributeValue::new)
        .toList();

    clothesAttributeDefinition = new ClothesAttributeDefinition(name, values);

    ReflectionTestUtils.setField(clothesAttributeDefinition, "id", definitionId);

    clothesAttributeDefDto = new ClothesAttributeDefDto(
        definitionId,
        name,
        selectableValues,
        CREATED_AT
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

  @Test
  @DisplayName("속성 등록 실패_중복된 속성 값")
  void createClothesAttributeDef_DuplicateValue() {
    // given
    List<String> duplicateValues = List.of("빨강", "빨강");
    ClothesAttributeDefCreateRequest request = new ClothesAttributeDefCreateRequest(name,
        duplicateValues);

    when(clothesAttributeDefRepository.existsByName(name)).thenReturn(false);

    // when & then
    assertThatThrownBy(() -> clothesAttributeDefService.create(request))
        .isInstanceOf(RestException.class)
        .hasMessage(ErrorCode.ATTRIBUTE_VALUE_DUPLICATE.getMessage());

    then(clothesAttributeDefRepository).should(never()).save(any());
  }

  @Test
  @DisplayName("이름순 정렬 + 키워드 검색으로 속성 목록 조회 성공")
  void getClothesAttributeDefs_Success() {
    // given
    ClothesAttributeDefSearchCondition condition = new ClothesAttributeDefSearchCondition(
        null,
        null,
        5,
        SortBy.NAME,
        SortDirection.ASCENDING,
        "감"
    );

    ClothesAttributeDefinition thicknessDef = new ClothesAttributeDefinition("두께감", List.of());
    ClothesAttributeDefinition patternDef = new ClothesAttributeDefinition("패턴", List.of());
    ClothesAttributeDefinition seasonDef = new ClothesAttributeDefinition("계절감", List.of());

    thicknessDef.addValue(new ClothesAttributeValue("얇음"));
    thicknessDef.addValue(new ClothesAttributeValue("두꺼움"));

    patternDef.addValue(new ClothesAttributeValue("줄무늬"));
    patternDef.addValue(new ClothesAttributeValue("땡땡이"));

    seasonDef.addValue(new ClothesAttributeValue("봄"));
    seasonDef.addValue(new ClothesAttributeValue("여름"));

    ReflectionTestUtils.setField(thicknessDef, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(patternDef, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(seasonDef, "id", UUID.randomUUID());

    ReflectionTestUtils.setField(thicknessDef, "createdAt", Instant.now());
    ReflectionTestUtils.setField(patternDef, "createdAt", Instant.now());
    ReflectionTestUtils.setField(seasonDef, "createdAt", Instant.now());

    given(clothesAttributeDefRepository.searchWithCursor(condition))
        .willReturn(List.of(seasonDef, thicknessDef));

    given(clothesAttributeDefRepository.countWithCondition(condition))
        .willReturn(2L);

    ClothesAttributeDefDto thicknessDefDto = new ClothesAttributeDefDto(
        thicknessDef.getId(),
        thicknessDef.getName(),
        List.of("얇음", "두꺼움"),
        thicknessDef.getCreatedAt()
    );
    ClothesAttributeDefDto seasonDefDto = new ClothesAttributeDefDto(
        seasonDef.getId(),
        seasonDef.getName(),
        List.of("봄", "여름"),
        seasonDef.getCreatedAt()
    );

    given(clothesAttributeDefMapper.toDto(thicknessDef)).willReturn(thicknessDefDto);
    given(clothesAttributeDefMapper.toDto(seasonDef)).willReturn(seasonDefDto);

    // when
    ClothesAttributeDefDtoCursorResponse result =
        clothesAttributeDefService.getClothesAttributeDefs(condition);

    // then
    assertThat(result).isNotNull();
    assertThat(result.data()).hasSize(2);

    assertThat(result.data())
        .extracting("name")
        .containsExactly("계절감", "두께감");

    assertThat(result.data().get(0).selectableValues())
        .containsExactly("봄", "여름");

    assertThat(result.data().get(1).selectableValues())
        .containsExactly("얇음", "두꺼움");

    assertThat(result.hasNext()).isFalse();
    assertThat(result.totalCount()).isEqualTo(2L);
  }

  @Test
  @DisplayName("의상 속성 수정 성공")
  void updateClothesAttributeDef_Success() {
    // given
    String newName = "새로운 색상";
    List<String> newValues = List.of("빨강", "노랑", "초록", "보라");
    ClothesAttributeDefUpdateRequest request =
        new ClothesAttributeDefUpdateRequest(newName, newValues);

    when(clothesAttributeDefRepository.findById(definitionId))
        .thenReturn(Optional.of(clothesAttributeDefinition));
    when(clothesAttributeDefRepository.existsByName(newName)).thenReturn(false);
    when(clothesAttributeDefRepository.save(clothesAttributeDefinition))
        .thenReturn(clothesAttributeDefinition);
    when(clothesAttributeDefMapper.toDto(clothesAttributeDefinition))
        .thenReturn(new ClothesAttributeDefDto(definitionId, newName, newValues, CREATED_AT));

    // when
    ClothesAttributeDefDto result = clothesAttributeDefService.update(definitionId, request);

    // then
    assertThat(result).isNotNull();
    assertThat(result.name()).isEqualTo(newName);
    assertThat(result.selectableValues()).containsExactlyElementsOf(newValues);

    verify(clothesAttributeDefRepository).findById(definitionId);
    verify(clothesAttributeDefRepository).save(clothesAttributeDefinition);
  }

  @Test
  @DisplayName("의상 속성 수정 실패_ 존재하지 않는 definitionId")
  void updateClothesAttributeDef_NotFound() {
    // given
    UUID nonExistentId = UUID.randomUUID();
    String newName = "새로운 색상";
    List<String> newValues = List.of("빨강", "노랑");

    ClothesAttributeDefUpdateRequest request =
        new ClothesAttributeDefUpdateRequest(newName, newValues);

    when(clothesAttributeDefRepository.findById(nonExistentId))
        .thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> clothesAttributeDefService.update(nonExistentId, request))
        .isInstanceOf(RestException.class)
        .hasMessage(ErrorCode.ATTRIBUTE_DEFINITION_NOT_FOUND.getMessage());

    verify(clothesAttributeDefRepository).findById(nonExistentId);
    verify(clothesAttributeDefRepository, never()).save(any());
  }

  @Test
  @DisplayName("의상 속성 삭제 성공")
  void deleteClothesAttributeDef_Success() {
    // given
    when(clothesAttributeDefRepository.findById(definitionId))
        .thenReturn(Optional.of(clothesAttributeDefinition));

    // when
    clothesAttributeDefService.delete(definitionId);

    // then
    verify(clothesAttributeDefRepository).findById(definitionId);
    verify(clothesAttributeDefRepository).delete(clothesAttributeDefinition);
  }

  @Test
  @DisplayName("의상 속성 삭제 실패_존재하지 않는 definitionId")
  void deleteClothesAttributeDef_NotFound() {
    // given
    UUID nonExistentId = UUID.randomUUID();
    when(clothesAttributeDefRepository.findById(nonExistentId))
        .thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> clothesAttributeDefService.delete(nonExistentId))
        .isInstanceOf(RestException.class)
        .hasMessage(ErrorCode.NOT_FOUND.getMessage());

    verify(clothesAttributeDefRepository).findById(nonExistentId);
    verify(clothesAttributeDefRepository, never()).delete(any());
  }
}