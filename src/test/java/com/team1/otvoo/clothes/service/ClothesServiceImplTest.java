package com.team1.otvoo.clothes.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.team1.otvoo.clothes.dto.ClothesCreateRequest;
import com.team1.otvoo.clothes.dto.ClothesDto;
import com.team1.otvoo.clothes.dto.ClothesDtoCursorResponse;
import com.team1.otvoo.clothes.dto.ClothesSearchCondition;
import com.team1.otvoo.clothes.dto.ClothesUpdateRequest;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDto;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeWithDefDto;
import com.team1.otvoo.clothes.entity.Clothes;
import com.team1.otvoo.clothes.entity.ClothesAttributeDefinition;
import com.team1.otvoo.clothes.entity.ClothesAttributeValue;
import com.team1.otvoo.clothes.entity.ClothesImage;
import com.team1.otvoo.clothes.entity.ClothesSelectedValue;
import com.team1.otvoo.clothes.entity.ClothesType;
import com.team1.otvoo.clothes.mapper.ClothesMapper;
import com.team1.otvoo.clothes.repository.ClothesAttributeDefRepository;
import com.team1.otvoo.clothes.repository.ClothesAttributeValueRepository;
import com.team1.otvoo.clothes.repository.ClothesImageRepository;
import com.team1.otvoo.clothes.repository.ClothesRepository;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.storage.S3ImageStorage;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
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
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ClothesServiceImplTest {

  @Mock
  private ClothesRepository clothesRepository;
  @Mock
  private ClothesAttributeDefRepository clothesAttributeDefRepository;
  @Mock
  private ClothesAttributeValueRepository clothesAttributeValueRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private ClothesMapper clothesMapper;
  @Mock
  private ClothesImageService clothesImageService;
  @Mock
  private ClothesImageRepository clothesImageRepository;
  @Mock
  private S3ImageStorage s3ImageStorage;

  @InjectMocks
  private ClothesServiceImpl clothesServiceImpl;

  private UUID ownerId;
  private User owner;

  private UUID colorDefId;
  private ClothesAttributeDefinition colorDef;
  private ClothesAttributeValue redVal, blueVal, greenVal;

  private UUID thicknessDefId;
  private ClothesAttributeDefinition thicknessDef;
  private ClothesAttributeValue thinVal, normalVal, thickVal;

  private Instant CREATED_AT;

  @BeforeEach
  void setUp() {
    ownerId = UUID.randomUUID();
    owner = User.builder().email("user@test.com").password("pw").build();
    ReflectionTestUtils.setField(owner, "id", ownerId);

    //색상
    colorDefId = UUID.randomUUID();
    redVal = new ClothesAttributeValue("빨강");
    blueVal = new ClothesAttributeValue("파랑");
    greenVal = new ClothesAttributeValue("초록");
    colorDef = new ClothesAttributeDefinition("색상", List.of(redVal, blueVal, greenVal));
    ReflectionTestUtils.setField(colorDef, "id", colorDefId);
    ReflectionTestUtils.setField(redVal, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(blueVal, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(greenVal, "id", UUID.randomUUID());

    // 두께감
    thicknessDefId = UUID.randomUUID();
    thinVal = new ClothesAttributeValue("얇음");
    normalVal = new ClothesAttributeValue("보통");
    thickVal = new ClothesAttributeValue("두꺼움");
    thicknessDef = new ClothesAttributeDefinition("두께감", List.of(thinVal, normalVal, thickVal));
    ReflectionTestUtils.setField(thicknessDef, "id", thicknessDefId);

    ReflectionTestUtils.setField(thinVal, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(normalVal, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(thickVal, "id", UUID.randomUUID());

    CREATED_AT = Instant.parse("2025-01-01T00:00:00Z");

  }

  @Test
  @DisplayName("의상 등록 성공")
  void createClothes_Success() {
    // given
    ClothesCreateRequest request = new ClothesCreateRequest(
        ownerId,
        "기본 티셔츠",
        ClothesType.TOP,
        List.of(
            new ClothesAttributeDto(colorDefId, "파랑"),
            new ClothesAttributeDto(thicknessDefId, "보통")
        )
    );

    given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));

    given(clothesAttributeDefRepository.findById(colorDefId)).willReturn(Optional.of(colorDef));
    given(clothesAttributeValueRepository.findByDefinitionIdAndValue(colorDefId, "파랑"))
        .willReturn(Optional.of(blueVal));
    given(clothesAttributeDefRepository.findById(thicknessDefId)).willReturn(
        Optional.of(thicknessDef));
    given(clothesAttributeValueRepository.findByDefinitionIdAndValue(thicknessDefId, "보통"))
        .willReturn(Optional.of(normalVal));

    Clothes saved = new Clothes(owner, "기본 티셔츠", ClothesType.TOP, List.of());
    UUID clothesId = UUID.randomUUID();
    ReflectionTestUtils.setField(saved, "id", clothesId);
    ReflectionTestUtils.setField(saved, "createdAt", CREATED_AT);

    ClothesImage stored = new ClothesImage(
        "images/clothes/" + clothesId + "/x.jpg",
        "x.jpg",
        "image/jpeg",
        123L, 100, 200,
        saved
    );

    MultipartFile imageFile = mock(MultipartFile.class);
    given(imageFile.isEmpty()).willReturn(false);

    given(clothesRepository.save(any(Clothes.class))).willReturn(saved);
    given(clothesImageService.create(saved, imageFile)).willReturn(stored);
    given(s3ImageStorage.getPresignedUrl(stored.getImageKey(), stored.getContentType()))
        .willReturn("https://example.com");

    ClothesDto expected = new ClothesDto(
        clothesId,
        ownerId,
        "기본 티셔츠",
        "https://example.com",
        ClothesType.TOP,
        List.of(
            new ClothesAttributeWithDefDto(colorDefId, "색상", List.of("빨강", "파랑", "초록"), "파랑"),
            new ClothesAttributeWithDefDto(thicknessDefId, "두께감", List.of("얇음", "보통", "두꺼움"), "보통")
        ),
        CREATED_AT
    );
    given(clothesMapper.toDto(saved, "https://example.com")).willReturn(expected);

    // when
    ClothesDto result = clothesServiceImpl.create(request, imageFile);

    // then
    assertThat(result).isNotNull();
    assertThat(result.name()).isEqualTo("기본 티셔츠");
    assertThat(result.ownerId()).isEqualTo(ownerId);
    assertThat(result.attributes()).hasSize(2);
    assertThat(result.attributes().get(0).definitionName()).isEqualTo("색상");
    assertThat(result.attributes().get(0).selectableValues()).containsExactly("빨강", "파랑", "초록");
    assertThat(result.attributes().get(0).value()).isEqualTo("파랑");
    assertThat(result.attributes().get(1).definitionName()).isEqualTo("두께감");
    assertThat(result.attributes().get(1).selectableValues()).containsExactly("얇음", "보통", "두꺼움");
    assertThat(result.attributes().get(1).value()).isEqualTo("보통");
    assertThat(result.imageUrl()).isEqualTo("https://example.com");

    then(clothesRepository).should().save(any(Clothes.class));
    then(clothesImageService).should().create(saved, imageFile);
    then(s3ImageStorage).should().getPresignedUrl(stored.getImageKey(), stored.getContentType());
    then(clothesMapper).should().toDto(saved, "https://example.com");
  }

  @Test
  @DisplayName("의상 등록 실패_속성에는 존재하지 않는 속성값 선택")
  void createClothes_Fail_AttributeValueNotFound() {
    // given: 색상 정의는 존재하지만 값 "보라"는 없음
    ClothesCreateRequest request = new ClothesCreateRequest(
        ownerId,
        "오류 티셔츠",
        ClothesType.TOP,
        List.of(new ClothesAttributeDto(colorDefId, "보라"))
    );

    given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
    given(clothesAttributeDefRepository.findById(colorDefId)).willReturn(Optional.of(colorDef));
    given(clothesAttributeValueRepository.findByDefinitionIdAndValue(colorDefId, "보라"))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> clothesServiceImpl.create(request, null))
        .isInstanceOf(RestException.class)
        .hasMessage(ErrorCode.ATTRIBUTE_VALUE_NOT_FOUND.getMessage());

    then(clothesRepository).should(never()).save(any());
  }

  @Test
  @DisplayName("의상 목록 조회 성공")
  void getClothes_Success() {
    // given
    int limit = 2;
    String cursor = null;
    UUID idAfter = null;

    ClothesSearchCondition condition = new ClothesSearchCondition(
        cursor,
        idAfter,
        limit,
        ClothesType.TOP,
        ownerId
    );

    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    UUID id3 = UUID.randomUUID();

    Clothes c1 = new Clothes(owner, "티셔츠1", ClothesType.TOP, List.of(
        new ClothesSelectedValue(colorDef, blueVal),
        new ClothesSelectedValue(thicknessDef, normalVal)
    ));
    Clothes c2 = new Clothes(owner, "티셔츠2", ClothesType.TOP, List.of(
        new ClothesSelectedValue(colorDef, greenVal),
        new ClothesSelectedValue(thicknessDef, thinVal)
    ));
    Clothes c3 = new Clothes(owner, "티셔츠3", ClothesType.TOP, List.of(
        new ClothesSelectedValue(colorDef, redVal),
        new ClothesSelectedValue(thicknessDef, thickVal)
    ));

    Instant t1 = Instant.parse("2025-01-03T00:00:00Z");
    Instant t2 = Instant.parse("2025-01-02T00:00:00Z");
    Instant t3 = Instant.parse("2025-01-01T00:00:00Z");

    ReflectionTestUtils.setField(c1, "id", id1);
    ReflectionTestUtils.setField(c1, "createdAt", t1);
    ReflectionTestUtils.setField(c2, "id", id2);
    ReflectionTestUtils.setField(c2, "createdAt", t2);
    ReflectionTestUtils.setField(c3, "id", id3);
    ReflectionTestUtils.setField(c3, "createdAt", t3);

    given(clothesRepository.searchWithCursor(condition)).willReturn(List.of(c1, c2, c3));
    given(clothesRepository.countWithCondition(condition)).willReturn(10L);
    given(clothesImageRepository.findAllByClothes_IdIn(anyList())).willReturn(List.of());

    ClothesDto d1 = new ClothesDto(
        id1, ownerId, "티셔츠1", null, ClothesType.TOP,
        List.of(
            new ClothesAttributeWithDefDto(colorDefId, "색상", List.of("빨강", "파랑", "초록"), "파랑"),
            new ClothesAttributeWithDefDto(thicknessDefId, "두께감", List.of("얇음", "보통", "두꺼움"), "보통")
        ),
        t1
    );
    ClothesDto d2 = new ClothesDto(
        id2, ownerId, "티셔츠2", null, ClothesType.TOP,
        List.of(
            new ClothesAttributeWithDefDto(colorDefId, "색상", List.of("빨강", "파랑", "초록"), "초록"),
            new ClothesAttributeWithDefDto(thicknessDefId, "두께감", List.of("얇음", "보통", "두꺼움"), "얇음")
        ),
        t2
    );

    given(clothesMapper.toDto(c1, null)).willReturn(d1);
    given(clothesMapper.toDto(c2, null)).willReturn(d2);

    // when
    ClothesDtoCursorResponse result = clothesServiceImpl.getClothesList(condition);

    // then
    assertThat(result).isNotNull();
    assertThat(result.hasNext()).isTrue();
    assertThat(result.totalCount()).isEqualTo(10L);
    assertThat(result.data()).hasSize(2);

    assertThat(result.data().get(0).name()).isEqualTo("티셔츠1");
    assertThat(result.data().get(0).attributes()).extracting(
            ClothesAttributeWithDefDto::definitionName)
        .containsExactlyInAnyOrder("색상", "두께감");
    assertThat(result.data().get(0).attributes().stream()
        .anyMatch(a -> a.definitionName().equals("색상") && a.value().equals("파랑"))).isTrue();
    assertThat(result.data().get(0).attributes().stream()
        .anyMatch(a -> a.definitionName().equals("두께감") && a.value().equals("보통"))).isTrue();

    assertThat(result.data().get(1).name()).isEqualTo("티셔츠2");
    assertThat(result.data().get(1).attributes().stream()
        .anyMatch(a -> a.definitionName().equals("색상") && a.value().equals("초록"))).isTrue();
    assertThat(result.data().get(1).attributes().stream()
        .anyMatch(a -> a.definitionName().equals("두께감") && a.value().equals("얇음"))).isTrue();

    assertThat(result.nextCursor()).isEqualTo(t2.toString());
    assertThat(result.nextIdAfter()).isEqualTo(id2);

    then(clothesMapper).should().toDto(c1, null);
    then(clothesMapper).should().toDto(c2, null);
    then(clothesMapper).should(never()).toDto(c3, null);
  }

  @Test
  @DisplayName("의상 목록 조회_의상 이미지 포함")
  void getClothes_Success_WithImage() {
    //given
    int limit = 2;
    String cursor = null;
    UUID idAfter = null;

    ClothesSearchCondition condition = new ClothesSearchCondition(
        cursor,
        idAfter,
        limit,
        ClothesType.OUTER,
        ownerId
    );

    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    UUID id3 = UUID.randomUUID();

    Clothes c1 = new Clothes(owner, "코트", ClothesType.OUTER, List.of(
        new ClothesSelectedValue(colorDef, redVal),
        new ClothesSelectedValue(thicknessDef, thickVal)
    ));
    Clothes c2 = new Clothes(owner, "패딩", ClothesType.OUTER, List.of(
        new ClothesSelectedValue(colorDef, blueVal),
        new ClothesSelectedValue(thicknessDef, thickVal)
    ));
    Clothes c3 = new Clothes(owner, "자켓", ClothesType.OUTER, List.of(
        new ClothesSelectedValue(colorDef, greenVal),
        new ClothesSelectedValue(thicknessDef, thinVal)
    ));

    Instant t1 = Instant.parse("2025-01-03T00:00:00Z");
    Instant t2 = Instant.parse("2025-01-02T00:00:00Z");
    Instant t3 = Instant.parse("2025-01-01T00:00:00Z");

    ReflectionTestUtils.setField(c1, "id", id1);
    ReflectionTestUtils.setField(c1, "createdAt", t1);
    ReflectionTestUtils.setField(c2, "id", id2);
    ReflectionTestUtils.setField(c2, "createdAt", t2);
    ReflectionTestUtils.setField(c3, "id", id3);
    ReflectionTestUtils.setField(c3, "createdAt", t3);

    given(clothesRepository.searchWithCursor(condition)).willReturn(List.of(c1, c2, c3));
    given(clothesRepository.countWithCondition(condition)).willReturn(10L);

    ClothesImage img1 = new ClothesImage(
        "images/clothes/" + id1 + "/a.jpg", "a.jpg", "image/jpeg", 111L, 100, 100, c1
    );
    ClothesImage img2 = new ClothesImage(
        "images/clothes/" + id2 + "/b.jpeg", "b.jpeg", "image/jpeg", 222L, 120, 140, c2
    );
    given(clothesImageRepository.findAllByClothes_IdIn(anyList())).willReturn(List.of(img1, img2));

    given(s3ImageStorage.getPresignedUrl(img1.getImageKey(), img1.getContentType()))
        .willReturn("https://img.example/1");
    given(s3ImageStorage.getPresignedUrl(img2.getImageKey(), img2.getContentType()))
        .willReturn("https://img.example/2");

    ClothesDto d1 = new ClothesDto(
        id1, ownerId, "코트", "https://img.example/1", ClothesType.OUTER,
        List.of(
            new ClothesAttributeWithDefDto(colorDefId, "색상", List.of("빨강", "파랑", "초록"), "빨강"),
            new ClothesAttributeWithDefDto(thicknessDefId, "두께감", List.of("얇음", "보통", "두꺼움"), "두꺼움")
        ),
        t1
    );
    ClothesDto d2 = new ClothesDto(
        id2, ownerId, "패딩", "https://img.example/2", ClothesType.OUTER,
        List.of(
            new ClothesAttributeWithDefDto(colorDefId, "색상", List.of("빨강", "파랑", "초록"), "파랑"),
            new ClothesAttributeWithDefDto(thicknessDefId, "두께감", List.of("얇음", "보통", "두꺼움"), "두꺼움")
        ),
        t2
    );

    given(clothesMapper.toDto(c1, "https://img.example/1")).willReturn(d1);
    given(clothesMapper.toDto(c2, "https://img.example/2")).willReturn(d2);

    // when
    ClothesDtoCursorResponse result = clothesServiceImpl.getClothesList(condition);

    // then
    assertThat(result).isNotNull();
    assertThat(result.hasNext()).isTrue();
    assertThat(result.totalCount()).isEqualTo(10L);
    assertThat(result.data()).hasSize(2);

    assertThat(result.data().get(0).name()).isEqualTo("코트");
    assertThat(result.data().get(0).imageUrl()).isEqualTo("https://img.example/1");
    assertThat(result.data().get(1).name()).isEqualTo("패딩");
    assertThat(result.data().get(1).imageUrl()).isEqualTo("https://img.example/2");

    assertThat(result.nextCursor()).isEqualTo(t2.toString());
    assertThat(result.nextIdAfter()).isEqualTo(id2);

    then(clothesImageRepository).should().findAllByClothes_IdIn(anyList());
    then(s3ImageStorage).should().getPresignedUrl(img1.getImageKey(), img1.getContentType());
    then(s3ImageStorage).should().getPresignedUrl(img2.getImageKey(), img2.getContentType());
    then(clothesMapper).should().toDto(c1, "https://img.example/1");
    then(clothesMapper).should().toDto(c2, "https://img.example/2");
    then(clothesMapper).should(never()).toDto(eq(c3), any());
  }

  @Test
  @DisplayName("의상 수정 성공")
  void updateClothes_Success() {
    // given: 기존 의상(색상=파랑, 두께감=보통)
    Clothes clothes = new Clothes(owner, "원래 티셔츠", ClothesType.TOP, List.of(
        new ClothesSelectedValue(colorDef, blueVal),     // 색상=파랑
        new ClothesSelectedValue(thicknessDef, normalVal) // 두께감=보통
    ));
    UUID clothesId = UUID.randomUUID();
    ReflectionTestUtils.setField(clothes, "id", clothesId);
    ReflectionTestUtils.setField(clothes, "createdAt", CREATED_AT);

    // 새로 추가할 정의(계절)
    UUID seasonDefId = UUID.randomUUID();
    ClothesAttributeValue springVal = new ClothesAttributeValue("봄");
    ClothesAttributeValue summerVal = new ClothesAttributeValue("여름");
    ClothesAttributeDefinition seasonDef = new ClothesAttributeDefinition("계절",
        List.of(springVal, summerVal));
    ReflectionTestUtils.setField(seasonDef, "id", seasonDefId);

    ReflectionTestUtils.setField(springVal, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(summerVal, "id", UUID.randomUUID());

    // 업데이트 요청:
    // - 이름 변경
    // - 타입 변경 TOP -> OUTER
    // - 색상: 파랑 -> 초록 (값 변경)
    // - 계절: 새로 추가 (추가)
    // - 두께감: 요청에서 누락 (삭제)
    ClothesUpdateRequest request = new ClothesUpdateRequest(
        "업데이트 티셔츠",
        ClothesType.OUTER,
        List.of(
            new ClothesAttributeDto(colorDefId, "초록"),
            new ClothesAttributeDto(seasonDefId, "여름")
        )
    );

    given(clothesRepository.findById(clothesId)).willReturn(Optional.of(clothes));
    given(clothesImageRepository.findByClothes_Id(clothesId)).willReturn(Optional.empty());

    // toSelectedValue: 색상(초록), 계절(여름) 조회
    given(clothesAttributeDefRepository.findById(colorDefId)).willReturn(Optional.of(colorDef));
    given(clothesAttributeValueRepository.findByDefinitionIdAndValue(colorDefId, "초록"))
        .willReturn(Optional.of(greenVal));

    given(clothesAttributeDefRepository.findById(seasonDefId)).willReturn(Optional.of(seasonDef));
    given(clothesAttributeValueRepository.findByDefinitionIdAndValue(seasonDefId, "여름"))
        .willReturn(Optional.of(summerVal));

    given(clothesRepository.save(any(Clothes.class))).willAnswer(
        invocation -> invocation.getArgument(0));

    ClothesDto expected = new ClothesDto(
        clothesId,
        ownerId,
        "업데이트 티셔츠",
        null,
        ClothesType.OUTER,
        List.of(
            new ClothesAttributeWithDefDto(colorDefId, "색상", List.of("빨강", "파랑", "초록"), "초록"),
            new ClothesAttributeWithDefDto(seasonDefId, "계절", List.of("봄", "여름"), "여름")
        ),
        CREATED_AT
    );
    given(clothesMapper.toDto(any(Clothes.class), isNull())).willReturn(expected);

    // when
    ClothesDto result = clothesServiceImpl.update(clothesId, request, null);

    // then
    assertThat(result).isNotNull();
    assertThat(result.name()).isEqualTo("업데이트 티셔츠");
    assertThat(result.type()).isEqualTo(ClothesType.OUTER);
    assertThat(result.attributes()).hasSize(2);
    assertThat(result.attributes().get(0).definitionName()).isIn("색상", "계절");
    assertThat(result.attributes().stream()
        .anyMatch(a -> a.definitionName().equals("색상") && a.value().equals("초록"))).isTrue();
    assertThat(result.attributes().stream()
        .anyMatch(a -> a.definitionName().equals("계절") && a.value().equals("여름"))).isTrue();
    assertThat(
        result.attributes().stream().noneMatch(a -> a.definitionName().equals("두께감"))).isTrue();

    then(clothesRepository).should().findById(clothesId);
    then(clothesRepository).should().save(any(Clothes.class));
    then(clothesMapper).should().toDto(any(Clothes.class), isNull());
  }

  @Test
  @DisplayName("의상 수정 성공_새로운 이미지로 교체")
  void updateClothes_Success_WithImage() {
    // 기존 의상
    Clothes clothes = new Clothes(owner, "원래 티셔츠", ClothesType.TOP, List.of(
        new ClothesSelectedValue(colorDef, blueVal),
        new ClothesSelectedValue(thicknessDef, normalVal)
    ));
    UUID clothesId = UUID.randomUUID();
    ReflectionTestUtils.setField(clothes, "id", clothesId);
    ReflectionTestUtils.setField(clothes, "createdAt", CREATED_AT);

    // 새 속성 정의(계절) + 색상 변경
    UUID seasonDefId = UUID.randomUUID();
    ClothesAttributeValue springVal = new ClothesAttributeValue("봄");
    ClothesAttributeValue summerVal = new ClothesAttributeValue("여름");
    ClothesAttributeDefinition seasonDef = new ClothesAttributeDefinition("계절",
        List.of(springVal, summerVal));
    ReflectionTestUtils.setField(seasonDef, "id", seasonDefId);
    ReflectionTestUtils.setField(springVal, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(summerVal, "id", UUID.randomUUID());

    ClothesUpdateRequest request = new ClothesUpdateRequest(
        "업데이트 티셔츠",
        ClothesType.OUTER,
        List.of(
            new ClothesAttributeDto(colorDefId, "초록"),
            new ClothesAttributeDto(seasonDefId, "여름")
        )
    );

    MultipartFile imageFile = mock(MultipartFile.class);
    given(imageFile.isEmpty()).willReturn(false);

    given(clothesRepository.findById(clothesId)).willReturn(Optional.of(clothes));
    given(clothesAttributeDefRepository.findById(colorDefId)).willReturn(Optional.of(colorDef));
    given(clothesAttributeValueRepository.findByDefinitionIdAndValue(colorDefId, "초록"))
        .willReturn(Optional.of(greenVal));
    given(clothesAttributeDefRepository.findById(seasonDefId)).willReturn(Optional.of(seasonDef));
    given(clothesAttributeValueRepository.findByDefinitionIdAndValue(seasonDefId, "여름"))
        .willReturn(Optional.of(summerVal));
    given(clothesRepository.save(any(Clothes.class))).willAnswer(inv -> inv.getArgument(0));

    // 이미지 교체 경로 stubs
    ClothesImage stored = new ClothesImage(
        "images/clothes/" + clothesId + "/new.jpg",
        "new.jpg",
        "image/jpeg",
        555L, 300, 400,
        clothes
    );
    given(clothesImageService.create(eq(clothes), eq(imageFile))).willReturn(stored);
    given(s3ImageStorage.getPresignedUrl(eq(stored.getImageKey()), eq(stored.getContentType())))
        .willReturn("https://example.com/new");

    // mapper
    ClothesDto expected = new ClothesDto(
        clothesId, ownerId, "업데이트 티셔츠", "https://example.com/new",
        ClothesType.OUTER,
        List.of(
            new ClothesAttributeWithDefDto(colorDefId, "색상", List.of("빨강", "파랑", "초록"), "초록"),
            new ClothesAttributeWithDefDto(seasonDefId, "계절", List.of("봄", "여름"), "여름")
        ),
        CREATED_AT
    );
    given(clothesMapper.toDto(eq(clothes), eq("https://example.com/new"))).willReturn(expected);

    // when
    ClothesDto result = clothesServiceImpl.update(clothesId, request, imageFile);

    // then
    assertThat(result.imageUrl()).isEqualTo("https://example.com/new");
    assertThat(result.type()).isEqualTo(ClothesType.OUTER);

    then(clothesRepository).should().findById(clothesId);
    then(clothesImageService).should().create(eq(clothes), eq(imageFile));
    then(s3ImageStorage).should()
        .getPresignedUrl(eq(stored.getImageKey()), eq(stored.getContentType()));
    then(clothesMapper).should().toDto(eq(clothes), eq("https://example.com/new"));
  }

  @Test
  @DisplayName("의상 수정 실패_같은 정의가 중복으로 전달")
  void updateClothes_Fail_DuplicateDefinition() {
    // given
    Clothes clothes = new Clothes(owner, "원래 티셔츠", ClothesType.TOP, List.of(
        new ClothesSelectedValue(colorDef, blueVal)
    ));
    UUID clothesId = UUID.randomUUID();
    ReflectionTestUtils.setField(clothes, "id", clothesId);
    ReflectionTestUtils.setField(clothes, "createdAt", CREATED_AT);

    // 같은 definitionId(colorDefId)가 두 번 들어오는 잘못된 요청
    ClothesUpdateRequest request = new ClothesUpdateRequest(
        "그냥 이름 변경",
        null,
        List.of(
            new ClothesAttributeDto(colorDefId, "빨강"),
            new ClothesAttributeDto(colorDefId, "초록")
        )
    );

    given(clothesRepository.findById(clothesId)).willReturn(Optional.of(clothes));

    // when & then
    assertThatThrownBy(() -> clothesServiceImpl.update(clothesId, request, null))
        .isInstanceOf(RestException.class)
        .hasMessage(ErrorCode.ATTRIBUTE_DEFINITION_DUPLICATE.getMessage());

    then(clothesRepository).should().findById(clothesId);
    then(clothesRepository).should(never()).save(any());
    then(clothesMapper).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("의상 삭제 성공")
  void deleteClothes_Success() {
    // given
    UUID clothesId = UUID.randomUUID();
    Clothes clothes = new Clothes(owner, "삭제 대상", ClothesType.TOP, List.of(
        new ClothesSelectedValue(colorDef, blueVal)
    ));
    ReflectionTestUtils.setField(clothes, "id", clothesId);

    ClothesImage image = new ClothesImage(
        "images/clothes/" + clothesId + "/a.jpg",
        "a.jpg",
        "image/jpeg",
        111L, 100, 100,
        clothes
    );

    given(clothesRepository.findById(clothesId)).willReturn(Optional.of(clothes));
    given(clothesImageRepository.findByClothes_Id(clothesId)).willReturn(Optional.of(image));

    // when
    assertThatCode(() -> clothesServiceImpl.delete(clothesId))
        .doesNotThrowAnyException();

    // then
    then(clothesRepository).should().findById(clothesId);
    then(clothesImageRepository).should().findByClothes_Id(clothesId);
    then(clothesImageService).should().delete(image);
    then(clothesRepository).should().delete(clothes);

  }

  @Test
  @DisplayName("의상 삭제 실패_존재하지 않는 clothesId")
  void deleteClothes_Fail_NotFound() {
    // given
    UUID clothesId = UUID.randomUUID();
    given(clothesRepository.findById(clothesId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> clothesServiceImpl.delete(clothesId))
        .isInstanceOf(RestException.class)
        .hasMessage(ErrorCode.CLOTHES_NOT_FOUND.getMessage());

    then(clothesRepository).should().findById(clothesId);
    then(clothesRepository).should(never()).delete(any());
  }
}

