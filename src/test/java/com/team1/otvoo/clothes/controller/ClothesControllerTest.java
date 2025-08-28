package com.team1.otvoo.clothes.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.otvoo.clothes.dto.ClothesCreateRequest;
import com.team1.otvoo.clothes.dto.ClothesDto;
import com.team1.otvoo.clothes.dto.ClothesDtoCursorResponse;
import com.team1.otvoo.clothes.dto.ClothesSearchCondition;
import com.team1.otvoo.clothes.dto.ClothesUpdateRequest;
import com.team1.otvoo.clothes.dto.SortBy;
import com.team1.otvoo.clothes.dto.SortDirection;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDto;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeWithDefDto;
import com.team1.otvoo.clothes.entity.ClothesType;
import com.team1.otvoo.clothes.extraction.service.ClothesExtractService;
import com.team1.otvoo.clothes.service.ClothesService;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.security.CustomUserDetailsService;
import com.team1.otvoo.security.JwtAuthenticationFilter;
import com.team1.otvoo.security.JwtTokenProvider;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ClothesController.class)
class ClothesControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private JwtTokenProvider jwtTokenProvider;
  @MockitoBean
  private CustomUserDetailsService customUserDetailsService;
  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;


  @MockitoBean
  private ClothesService clothesService;
  @MockitoBean
  private ClothesExtractService clothesExtractService;

  private static final Instant CREATED_AT = Instant.parse("2025-01-01T00:00:00Z");

  @Test
  @DisplayName("의상 등록 성공")
  void createClothes_Success() throws Exception {
    // given
    UUID ownerId = UUID.randomUUID();
    ClothesCreateRequest request = new ClothesCreateRequest(
        ownerId,
        "기본 티셔츠",
        ClothesType.TOP,
        List.of(new ClothesAttributeDto(UUID.randomUUID(), "블랙"))
    );

    MockMultipartFile clothesPart = new MockMultipartFile(
        "request", "",
        MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsBytes(request));

    ClothesDto responseDto = new ClothesDto(
        UUID.randomUUID(),
        ownerId,
        "기본 티셔츠",
        null,
        ClothesType.TOP,
        List.of(
            new ClothesAttributeWithDefDto(
                UUID.randomUUID(), "색상", List.of("레드", "블랙"), "블랙"
            )
        ),
        CREATED_AT
    );

    when(clothesService.create(any(), any())).thenReturn(responseDto);

    // when & then
    mockMvc.perform(multipart("/api/clothes")
            .file(clothesPart)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("기본 티셔츠"))
        .andExpect(jsonPath("$.ownerId").value(ownerId.toString()))
        .andExpect(jsonPath("$.type").value(ClothesType.TOP.name()))
        .andExpect(jsonPath("$.attributes[0].definitionName").value("색상"));

    verify(clothesService).create(eq(request), isNull());
  }

  @Test
  @DisplayName("의상 등록 성공_이미지 포함")
  void createClothes_WithImage_Success() throws Exception {
    UUID ownerId = UUID.randomUUID();
    ClothesCreateRequest request = new ClothesCreateRequest(
        ownerId, "기본 티셔츠", ClothesType.TOP,
        List.of(new ClothesAttributeDto(UUID.randomUUID(), "블랙"))
    );

    MockMultipartFile reqPart = new MockMultipartFile(
        "request", "", MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsBytes(request)
    );
    MockMultipartFile imgPart = new MockMultipartFile(
        "image", "x.jpg", "image/jpeg", "dummy".getBytes()
    );

    ClothesDto responseDto = new ClothesDto(
        UUID.randomUUID(), ownerId, "기본 티셔츠", "https://example.com",
        ClothesType.TOP,
        List.of(new ClothesAttributeWithDefDto(UUID.randomUUID(), "색상", List.of("레드", "블랙"), "블랙")),
        CREATED_AT
    );
    when(clothesService.create(any(), any())).thenReturn(responseDto);

    mockMvc.perform(multipart("/api/clothes")
            .file(reqPart)
            .file(imgPart)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.imageUrl").value("https://example.com"));

    verify(clothesService).create(
        eq(request),
        argThat(mf -> mf != null && !mf.isEmpty()
            && "x.jpg".equals(mf.getOriginalFilename())
            && "image/jpeg".equals(mf.getContentType()))
    );
  }

  @Test
  @DisplayName("의상 목록 조회 성공")
  void getClothes_Success() throws Exception {
    // given
    UUID ownerId = UUID.randomUUID();
    int limit = 2;

    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    Instant t1 = Instant.parse("2025-01-03T00:00:00Z");
    Instant t2 = Instant.parse("2025-01-02T00:00:00Z");

    ClothesDto dto1 = new ClothesDto(
        id1, ownerId, "티셔츠1",
        "https://example.com/1.jpg",
        ClothesType.TOP,
        List.of(
            new ClothesAttributeWithDefDto(UUID.randomUUID(), "색상", List.of("빨강", "파랑", "초록"), "파랑")
        ),
        t1
    );
    ClothesDto dto2 = new ClothesDto(
        id2, ownerId, "티셔츠2",
        "https://example.com/2.jpg",
        ClothesType.TOP,
        List.of(
            new ClothesAttributeWithDefDto(UUID.randomUUID(), "두께감", List.of("얇음", "보통", "두꺼움"),
                "얇음")
        ),
        t2
    );

    ClothesDtoCursorResponse responseDto = new ClothesDtoCursorResponse(
        List.of(dto1, dto2),
        t2.toString(),
        id2,
        true,
        10L,
        SortBy.CREATED_AT,
        SortDirection.DESCENDING
    );

    when(clothesService.getClothesList(Mockito.any(ClothesSearchCondition.class)))
        .thenReturn(responseDto);

    // when & then
    mockMvc.perform(get("/api/clothes")
            .param("ownerId", ownerId.toString())
            .param("typeEqual", "TOP")
            .param("limit", String.valueOf(limit))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasNext").value(true))
        .andExpect(jsonPath("$.totalCount").value(10))
        .andExpect(jsonPath("$.data", hasSize(2)))
        .andExpect(jsonPath("$.data[0].name").value("티셔츠1"))
        .andExpect(jsonPath("$.data[1].name").value("티셔츠2"))
        .andExpect(jsonPath("$.data[0].imageUrl").value("https://example.com/1.jpg"))
        .andExpect(jsonPath("$.data[1].imageUrl").value("https://example.com/2.jpg"))
        .andExpect(jsonPath("$.nextCursor").value(t2.toString()))
        .andExpect(jsonPath("$.nextIdAfter").value(id2.toString()))
        .andExpect(jsonPath("$.sortBy").value("createdAt"))
        .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));
  }

  @Test
  @DisplayName("의상 수정 성공")
  void updateClothes_Success() throws Exception {
    // given
    UUID clothesId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    ClothesUpdateRequest request = new ClothesUpdateRequest(
        "업데이트 티셔츠",
        ClothesType.OUTER,
        List.of(new ClothesAttributeDto(UUID.randomUUID(), "봄"))
    );
    MockMultipartFile clothesPart = new MockMultipartFile(
        "request", "",
        MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsBytes(request));

    ClothesDto responseDto = new ClothesDto(
        clothesId,
        ownerId,
        "업데이트 티셔츠",
        null,
        ClothesType.OUTER,
        List.of(),
        CREATED_AT
    );
    when(clothesService.update(any(), eq(clothesId), any(), any()))
        .thenReturn(responseDto);

    // when & then
    mockMvc.perform(multipart("/api/clothes/{clothesId}", clothesId)
            .file(clothesPart)
            .with(r -> {
              r.setMethod("PATCH");
              return r;
            })
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(clothesId.toString()))
        .andExpect(jsonPath("$.ownerId").value(ownerId.toString()))
        .andExpect(jsonPath("$.name").value("업데이트 티셔츠"))
        .andExpect(jsonPath("$.type").value(ClothesType.OUTER.name()))
        .andExpect(jsonPath("$.attributes", hasSize(0))
        );
    verify(clothesService).update(any(), eq(clothesId), eq(request), isNull());
  }

  @Test
  @DisplayName("의상 수정 성공_이미지 포함")
  void updateClothes_WithImage_Success() throws Exception {
    UUID clothesId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();

    ClothesUpdateRequest request = new ClothesUpdateRequest(
        "업데이트 티셔츠", ClothesType.OUTER,
        List.of(new ClothesAttributeDto(UUID.randomUUID(), "여름"))
    );

    MockMultipartFile reqPart = new MockMultipartFile(
        "request", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(request));
    MockMultipartFile imgPart = new MockMultipartFile(
        "image", "new.jpg", "image/jpeg", "dummy".getBytes());

    ClothesDto responseDto = new ClothesDto(
        clothesId, ownerId, "업데이트 티셔츠", "https://img/new",
        ClothesType.OUTER, List.of(), CREATED_AT
    );
    when(clothesService.update(any(), eq(clothesId), Mockito.any(), Mockito.any()))
        .thenReturn(responseDto);

    mockMvc.perform(multipart("/api/clothes/{clothesId}", clothesId)
            .file(reqPart)
            .file(imgPart)
            .with(r -> {
              r.setMethod("PATCH");
              return r;
            })
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.imageUrl").value("https://img/new"));

    verify(clothesService).update(
        any(),
        eq(clothesId),
        any(ClothesUpdateRequest.class),
        argThat(
            mf -> mf != null && !mf.isEmpty() && "new.jpg".equals(mf.getOriginalFilename())
                && "image/jpeg".equals(mf.getContentType()))
    );
  }

  @Test
  @DisplayName("의상 수정 실패_같은 속성 정의가 중복 전달")
  void updateClothes_Fail_DuplicateDefinition() throws Exception {
    // given
    UUID clothesId = UUID.randomUUID();
    UUID defId = UUID.randomUUID();
    ClothesUpdateRequest request = new ClothesUpdateRequest(
        "색상",
        null,
        List.of(
            new ClothesAttributeDto(defId, "빨강"),
            new ClothesAttributeDto(defId, "파랑")
        )
    );
    MockMultipartFile clothesPart = new MockMultipartFile(
        "request", "",
        MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsBytes(request)
    );

    when(clothesService.update(any(), eq(clothesId), any(), any()))
        .thenThrow(new RestException(ErrorCode.ATTRIBUTE_DEFINITION_DUPLICATE));

    // when & then
    mockMvc.perform(multipart("/api/clothes/{clothesId}", clothesId)
            .file(clothesPart)
            .with(r -> {
              r.setMethod("PATCH");
              return r;
            }))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("의상 삭제 성공")
  void deleteClothes_Success() throws Exception {
    // given
    UUID clothesId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/clothes/{clothesId}", clothesId)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    verify(clothesService).delete(any(), eq(clothesId));
  }

  @Test
  @DisplayName("의상 삭제 실패_존재하지 않는 clothesId")
  void deleteClothes_Fail_NotFound() throws Exception {
    // given
    UUID clothesId = UUID.randomUUID();
    doThrow(new RestException(ErrorCode.CLOTHES_NOT_FOUND))
        .when(clothesService).delete(any(), eq(clothesId));

    // when & then
    mockMvc.perform(delete("/api/clothes/{clothesId}", clothesId)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("의상 추출 성공")
  void extractClothes_Success() throws Exception {
    String url = "http://shop.example/item/123";

    ClothesDto responseDto = new ClothesDto(
        null,
        null,
        "코트",
        "https://img.example/coat.jpg",
        ClothesType.OUTER,
        List.of(new ClothesAttributeWithDefDto(
            UUID.randomUUID(), "색상", List.of("블랙", "그레이"), "블랙"
        )),
        CREATED_AT
    );
    when(clothesExtractService.extract(url)).thenReturn(responseDto);

    mockMvc.perform(get("/api/clothes/extractions")
            .param("url", url)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("코트"))
        .andExpect(jsonPath("$.type").value(ClothesType.OUTER.name()))
        .andExpect(jsonPath("$.imageUrl").value("https://img.example/coat.jpg"));

    verify(clothesExtractService).extract(url);
  }

  @Test
  @DisplayName("의상 추출 실패_url 공백")
  void extractClothes_Fail_BlankUrl() throws Exception {
    mockMvc.perform(get("/api/clothes/extractions")
            .param("url", " ")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }
}