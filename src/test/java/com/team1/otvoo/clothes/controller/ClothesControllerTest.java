package com.team1.otvoo.clothes.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.otvoo.clothes.dto.ClothesCreateRequest;
import com.team1.otvoo.clothes.dto.ClothesDto;
import com.team1.otvoo.clothes.dto.ClothesUpdateRequest;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDto;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeWithDefDto;
import com.team1.otvoo.clothes.entity.ClothesType;
import com.team1.otvoo.clothes.service.ClothesService;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
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
  private ClothesService clothesService;

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

    Mockito.when(clothesService.create(any(), any())).thenReturn(responseDto);

    // when & then
    mockMvc.perform(multipart("/api/clothes")
            .file(clothesPart)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("기본 티셔츠"))
        .andExpect(jsonPath("$.ownerId").value(ownerId.toString()))
        .andExpect(jsonPath("$.type").value(ClothesType.TOP.name()))
        .andExpect(jsonPath("$.attributes[0].definitionName").value("색상"));
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
    Mockito.when(clothesService.update(Mockito.eq(clothesId), any(), any()))
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

    Mockito.when(clothesService.update(Mockito.eq(clothesId), any(), any()))
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

    Mockito.verify(clothesService).delete(clothesId);
  }

  @Test
  @DisplayName("의상 삭제 실패_존재하지 않는 clothesId")
  void deleteClothes_Fail_NotFound() throws Exception {
    // given
    UUID clothesId = UUID.randomUUID();
    Mockito.doThrow(new RestException(ErrorCode.CLOTHES_NOT_FOUND))
        .when(clothesService).delete(clothesId);

    // when & then
    mockMvc.perform(delete("/api/clothes/{clothesId}", clothesId)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }
}