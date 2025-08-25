package com.team1.otvoo.clothes.controller;

import com.team1.otvoo.clothes.dto.ClothesCreateRequest;
import com.team1.otvoo.clothes.dto.ClothesDto;
import com.team1.otvoo.clothes.dto.ClothesDtoCursorResponse;
import com.team1.otvoo.clothes.dto.ClothesSearchCondition;
import com.team1.otvoo.clothes.dto.ClothesUpdateRequest;
import com.team1.otvoo.clothes.entity.ClothesType;
import com.team1.otvoo.clothes.extraction.service.ClothesExtractService;
import com.team1.otvoo.clothes.service.ClothesService;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.security.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clothes")
public class ClothesController {

  private final ClothesService clothesService;
  private final ClothesExtractService clothesExtractService;

  @PreAuthorize("#request.ownerId() == principal.user.id")
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

  @PreAuthorize("#ownerId == principal.user.id")
  @GetMapping
  public ResponseEntity<ClothesDtoCursorResponse> getClothes(
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam String typeEqual,
      @RequestParam UUID ownerId
  ) {
    log.info("의상 목록 조회 요청");

    if (limit <= 0) {
      throw new RestException(ErrorCode.INVALID_INPUT_VALUE,
          Map.of("limit", limit));
    }
    if (cursor == null ^ idAfter == null) {
      throw new RestException(ErrorCode.MISSING_REQUEST_PARAMETER,
          Map.of("cursor", cursor, "idAfter", idAfter));
    }
    ClothesType type = ClothesType.fromString(typeEqual);

    ClothesSearchCondition condition = new ClothesSearchCondition(
        cursor,
        idAfter,
        limit,
        type,
        ownerId
    );
    ClothesDtoCursorResponse result = clothesService.getClothesList(condition);

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(result);
  }

  @PatchMapping(
      path = "/{clothesId}",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ClothesDto> update(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable UUID clothesId,
      @RequestPart("request") ClothesUpdateRequest request,
      @RequestPart(value = "image", required = false) MultipartFile image) {
    log.info("의상 수정 요청 - clothesId:{}", clothesId);

    ClothesDto result = clothesService.update(userDetails, clothesId, request, image);

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(result);
  }

  @DeleteMapping("/{clothesId}")
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable UUID clothesId) {
    log.info("의상 삭제 요청 - clothesId:{}", clothesId);

    clothesService.delete(userDetails, clothesId);

    return ResponseEntity.noContent().build();
  }

  @GetMapping("/extractions")
  public ResponseEntity<ClothesDto> extract(
      @RequestParam @NotBlank(message = "url은 비어있을 수 없습니다.") String url) {
    log.info("의상 정보 추출 요청 - url:{}", url);
    ClothesDto result = clothesExtractService.extract(url);

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(result);
  }
}
