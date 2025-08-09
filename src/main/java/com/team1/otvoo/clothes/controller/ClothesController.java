package com.team1.otvoo.clothes.controller;

import com.team1.otvoo.clothes.dto.ClothesCreateRequest;
import com.team1.otvoo.clothes.dto.ClothesDto;
import com.team1.otvoo.clothes.dto.ClothesUpdateRequest;
import com.team1.otvoo.clothes.service.ClothesService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clothes")
public class ClothesController {

  private final ClothesService clothesService;

  @PostMapping(
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ClothesDto> create(
      @Valid @RequestPart("request") ClothesCreateRequest request,
      @RequestPart(value = "image", required = false) MultipartFile image) {
    log.info("의상 등록 요청 - ownerId:{}, name:{}, type:{}, attrs:{}",
        request.ownerId(), request.name(), request.type(),
        request.attributes() == null ? 0 : request.attributes().size());

    ClothesDto result = clothesService.create(request, image);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(result);
  }

  @PatchMapping(
      path = "/{clothesId}",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ClothesDto> update(
      @PathVariable UUID clothesId,
      @RequestPart("request") ClothesUpdateRequest request,
      @RequestPart(value = "image", required = false) MultipartFile image) {
    log.info("의상 수정 요청 - clothesId:{}", clothesId);

    ClothesDto result = clothesService.update(clothesId, request, image);

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(result);
  }

  @DeleteMapping("/{clothesId}")
  public ResponseEntity<Void> delete(@PathVariable UUID clothesId) {
    log.info("의상 삭제 요청 - clothesId:{}", clothesId);

    clothesService.delete(clothesId);

    return ResponseEntity.noContent().build();
  }
}
