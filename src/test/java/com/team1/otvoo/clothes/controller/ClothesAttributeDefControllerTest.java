package com.team1.otvoo.clothes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.otvoo.clothes.dto.ClothesAttributeDefCreateRequest;
import com.team1.otvoo.clothes.dto.ClothesAttributeDefDto;
import com.team1.otvoo.clothes.dto.ClothesAttributeDefUpdateRequest;
import com.team1.otvoo.clothes.service.ClothesAttributeDefService;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import java.util.List;
import java.util.Map;
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
public class ClothesAttributeDefControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ClothesAttributeDefService clothesAttributeDefService;

  @Test
  @DisplayName("의상 속성 등록 성공")
  void createClothesAttributeDef_Success() throws Exception {
    // given
    ClothesAttributeDefCreateRequest request =
        new ClothesAttributeDefCreateRequest("색상", List.of("빨강", "파랑"));
    ClothesAttributeDefDto responseDto =
        new ClothesAttributeDefDto(UUID.randomUUID(), "색상", List.of("빨강", "파랑"));

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
  @DisplayName("의상 속성 수정 성공")
  void updateClothesAttributeDef_Success() throws Exception {
    // given
    UUID id = UUID.randomUUID();
    ClothesAttributeDefCreateRequest request =
        new ClothesAttributeDefCreateRequest("사이즈", List.of("S", "M", "L"));

    ClothesAttributeDefDto responseDto =
        new ClothesAttributeDefDto(id, "사이즈", List.of("S", "M", "L"));

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
        .thenThrow(new RestException(ErrorCode.NOT_FOUND));

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

