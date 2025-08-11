package com.team1.otvoo.clothes.controller;

import com.team1.otvoo.clothes.dto.SortBy;
import com.team1.otvoo.clothes.dto.SortDirection;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDefCreateRequest;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDefDto;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDefDtoCursorResponse;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDefSearchCondition;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDefUpdateRequest;
import com.team1.otvoo.clothes.service.ClothesAttributeDefService;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/clothes/attribute-defs")
@RequiredArgsConstructor
public class ClothesAttributeDefController {

  private final ClothesAttributeDefService clothesAttributeDefService;

  @PostMapping
  public ResponseEntity<ClothesAttributeDefDto> create(
      @RequestBody @Valid ClothesAttributeDefCreateRequest request) {
    log.info("의상 속성 등록 요청 - name: {}", request.name());

    ClothesAttributeDefDto result = clothesAttributeDefService.create(request);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(result);
  }

  @GetMapping
  public ResponseEntity<ClothesAttributeDefDtoCursorResponse> getClothesAttributeDefs(
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam String sortBy,
      @RequestParam String sortDirection,
      @RequestParam(required = false) String keywordLike) {
    log.info("의상 속성 목록 조회 요청");

    if (limit <= 0) {
      throw new RestException(ErrorCode.INVALID_INPUT_VALUE,
          Map.of("limit", limit));
    }

    SortBy sortByEnum = SortBy.from(sortBy);
    switch (sortByEnum) {
      case NAME -> {
        if (cursor == null && idAfter != null) {
          throw new RestException(ErrorCode.MISSING_REQUEST_PARAMETER,
              Map.of("cursor", cursor, "idAfter", idAfter));
        }
      }
      case CREATED_AT -> {
        if (cursor == null ^ idAfter == null) {
          throw new RestException(ErrorCode.MISSING_REQUEST_PARAMETER,
              Map.of("cursor", cursor, "idAfter", idAfter));
        }
      }
    }

    SortDirection sortDirectionEnum = SortDirection.from(sortDirection);

    ClothesAttributeDefSearchCondition condition = new ClothesAttributeDefSearchCondition(
        cursor,
        idAfter,
        limit,
        sortByEnum,
        sortDirectionEnum,
        keywordLike
    );
    ClothesAttributeDefDtoCursorResponse result =
        clothesAttributeDefService.getClothesAttributeDefs(condition);

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(result);
  }

  @PatchMapping("/{definitionId}")
  public ResponseEntity<ClothesAttributeDefDto> update(
      @PathVariable UUID definitionId,
      @RequestBody @Valid ClothesAttributeDefUpdateRequest request
  ) {
    log.info("의상 속성 수정 요청 - id: {}", definitionId);

    ClothesAttributeDefDto result = clothesAttributeDefService.update(definitionId, request);

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(result);
  }

  @DeleteMapping("/{definitionId}")
  public ResponseEntity<Void> delete(@PathVariable UUID definitionId) {
    log.info("의상 속성 삭제 요청 - id: {}", definitionId);

    clothesAttributeDefService.delete(definitionId);
    return ResponseEntity.noContent().build();
  }


}
