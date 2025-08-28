package com.team1.otvoo.clothes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.otvoo.clothes.dto.SortBy;
import com.team1.otvoo.clothes.dto.SortDirection;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDefCreateRequest;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDefDto;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDefDtoCursorResponse;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDefUpdateRequest;
import com.team1.otvoo.clothes.service.ClothesAttributeDefService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ClothesAttributeDefController.class)
class ClothesAttributeDefControllerTest {

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
  private ClothesAttributeDefService clothesAttributeDefService;

  private static final Instant CREATED_AT = Instant.parse("2025-01-01T00:00:00Z");

  @Test
  @DisplayName("의상 속성 등록 성공")
  void createClothesAttributeDef_Success() throws Exception {
    // given
    ClothesAttributeDefCreateRequest request =
        new ClothesAttributeDefCreateRequest("색상", List.of("빨강", "파랑"));
    ClothesAttributeDefDto responseDto =
        new ClothesAttributeDefDto(UUID.randomUUID(), "색상", List.of("빨강", "파랑"), CREATED_AT);

    Mockito.when(clothesAttributeDefService.create(any()))
        .thenReturn(responseDto);

    // when & then
    mockMvc.perform(post("/api/clothes/attribute-defs")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("색상"))
        .andExpect(jsonPath("$.selectableValues[0]").value("빨강"))
        .andExpect(jsonPath("$.selectableValues[1]").value("파랑"));
  }

  @Test
  @DisplayName("의상 속성 목록 조회 성공")
  void getClothesAttributeDefs_Success() throws Exception {
    // given
    ClothesAttributeDefDto def1 = new ClothesAttributeDefDto(
        UUID.randomUUID(),
        "계절감",
        List.of("봄", "여름"),
        CREATED_AT
    );
    ClothesAttributeDefDto def2 = new ClothesAttributeDefDto(
        UUID.randomUUID(),
        "두께감",
        List.of("얇음", "두꺼움"),
        CREATED_AT
    );

    ClothesAttributeDefDtoCursorResponse response = new ClothesAttributeDefDtoCursorResponse(
        List.of(def1, def2),
        null,
        null,
        false,
        2L,
        SortBy.NAME,
        SortDirection.ASCENDING
    );

    Mockito.when(clothesAttributeDefService.getClothesAttributeDefs(Mockito.any()))
        .thenReturn(response);

    // when & then
    mockMvc.perform(get("/api/clothes/attribute-defs")
            .param("limit", "5")
            .param("sortBy", "NAME")
            .param("sortDirection", "ASCENDING")
            .param("keywordLike", "감"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0].name").value("계절감"))
        .andExpect(jsonPath("$.data[0].selectableValues[0]").value("봄"))
        .andExpect(jsonPath("$.data[0].selectableValues[1]").value("여름"))
        .andExpect(jsonPath("$.data[1].name").value("두께감"))
        .andExpect(jsonPath("$.data[1].selectableValues[0]").value("얇음"))
        .andExpect(jsonPath("$.data[1].selectableValues[1]").value("두꺼움"))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.totalCount").value(2));
  }

  @Test
  @DisplayName("의상 속성 목록 조회 실패 - 유효하지 않은 sortBy 값")
  void getClothesAttributeDefs_InvalidSortBy() throws Exception {
    mockMvc.perform(get("/api/clothes/attribute-defs")
            .param("limit", "5")
            .param("sortBy", "UNKNOWN")
            .param("sortDirection", "ASCENDING"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionName").value("INVALID_SORT_BY_FIELD"));
  }

  @Test
  @DisplayName("의상 속성 수정 성공")
  void updateClothesAttributeDef_Success() throws Exception {
    // given
    UUID id = UUID.randomUUID();
    ClothesAttributeDefUpdateRequest request =
        new ClothesAttributeDefUpdateRequest("사이즈", List.of("S", "M", "L"));

    ClothesAttributeDefDto responseDto =
        new ClothesAttributeDefDto(id, "사이즈", List.of("S", "M", "L"), CREATED_AT);

    Mockito.when(clothesAttributeDefService.update(Mockito.eq(id), any()))
        .thenReturn(responseDto);

    // when & then
    mockMvc.perform(patch("/api/clothes/attribute-defs/{definitionId}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("사이즈"))
        .andExpect(jsonPath("$.selectableValues[0]").value("S"))
        .andExpect(jsonPath("$.selectableValues[1]").value("M"))
        .andExpect(jsonPath("$.selectableValues[2]").value("L"));
  }

  @Test
  @DisplayName("의상 속성 수정 실패_존재하지 않는 definitionId")
  void updateClothesAttributeDef_NotFound() throws Exception {
    // given
    UUID id = UUID.randomUUID();
    ClothesAttributeDefUpdateRequest request =
        new ClothesAttributeDefUpdateRequest("사이즈", List.of("S", "M", "L"));

    Mockito.when(clothesAttributeDefService.update(Mockito.eq(id), any()))
        .thenThrow(new RestException(ErrorCode.ATTRIBUTE_DEFINITION_NOT_FOUND));

    // when & then
    mockMvc.perform(patch("/api/clothes/attribute-defs/{definitionId}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("의상 속성 삭제 성공")
  void deleteClothesAttributeDef_Success() throws Exception {
    // given
    UUID id = UUID.randomUUID();
    Mockito.doNothing().when(clothesAttributeDefService).delete(id);

    // when & then
    mockMvc.perform(delete("/api/clothes/attribute-defs/{definitionId}", id))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("의상 속성 삭제 실패_존재하지 않는 definitionId")
  void deleteClothesAttributeDef_NotFound() throws Exception {
    UUID id = UUID.randomUUID();

    Mockito.doThrow(new RestException(ErrorCode.NOT_FOUND))
        .when(clothesAttributeDefService).delete(id);

    mockMvc.perform(delete("/api/clothes/attribute-defs/{definitionId}", id))
        .andExpect(status().isNotFound());
  }
}