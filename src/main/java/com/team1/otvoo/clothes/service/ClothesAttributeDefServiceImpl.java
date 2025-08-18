package com.team1.otvoo.clothes.service;

import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDefCreateRequest;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDefDto;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDefDtoCursorResponse;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDefSearchCondition;
import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDefUpdateRequest;
import com.team1.otvoo.clothes.entity.ClothesAttributeDefinition;
import com.team1.otvoo.clothes.entity.ClothesAttributeValue;
import com.team1.otvoo.clothes.event.ClothesAttributeEvent;
import com.team1.otvoo.clothes.mapper.ClothesAttributeDefMapper;
import com.team1.otvoo.clothes.repository.ClothesAttributeDefRepository;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesAttributeDefServiceImpl implements ClothesAttributeDefService {

  private final ClothesAttributeDefRepository clothesAttributeDefRepository;
  private final ClothesAttributeDefMapper clothesAttributeDefMapper;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public ClothesAttributeDefDto create(ClothesAttributeDefCreateRequest request) {
    log.info("의상 속성 등록 요청: name = {}, selectableValues = {}",
        request.name(), request.selectableValues());

    checkDuplicateName(request.name());
    List<String> requestValues = request.selectableValues() == null
        ? List.of()
        : request.selectableValues();
    checkDuplicateValue(requestValues);

    ClothesAttributeDefinition clothesAttributeDefinition = new ClothesAttributeDefinition(
        request.name(), new ArrayList<>());

    if (!requestValues.isEmpty()) {
      requestValues.forEach(value ->
          clothesAttributeDefinition.addValue(new ClothesAttributeValue(value))
      );
    }

    ClothesAttributeDefinition saved = clothesAttributeDefRepository.save(
        clothesAttributeDefinition);

    log.info("의상 속성 등록 완료 - id: {}, name: {}, values: {}", saved.getId(), saved.getName(),
        saved.getValues());

    eventPublisher.publishEvent(new ClothesAttributeEvent("CREATE", saved));

    return clothesAttributeDefMapper.toDto(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public ClothesAttributeDefDtoCursorResponse getClothesAttributeDefs(
      ClothesAttributeDefSearchCondition condition) {
    List<ClothesAttributeDefinition> clothesAttributeDefs =
        clothesAttributeDefRepository.searchWithCursor(condition);

    boolean hasNext = clothesAttributeDefs.size() > condition.limit();
    List<ClothesAttributeDefinition> page =
        hasNext ? clothesAttributeDefs.subList(0, condition.limit()) : clothesAttributeDefs;

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (hasNext) {
      ClothesAttributeDefinition last = page.get(page.size() - 1);
      nextCursor =
          switch (condition.sortBy()) {
            case NAME -> last.getName();
            case CREATED_AT -> last.getCreatedAt().toString();
          };
      nextIdAfter = last.getId();
    }

    long totalCount = clothesAttributeDefRepository.countWithCondition(condition);

    List<ClothesAttributeDefDto> data = page.stream()
        .map(clothesAttributeDefMapper::toDto)
        .toList();

    return new ClothesAttributeDefDtoCursorResponse(
        data,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        condition.sortBy(),
        condition.sortDirection()
    );
  }

  @Override
  @Transactional
  public ClothesAttributeDefDto update(UUID definitionId,
      ClothesAttributeDefUpdateRequest request) {
    log.info("의상 속성 수정 요청: id = {}, name = {}, selectableValues = {}",
        definitionId, request.name(), request.selectableValues());

    ClothesAttributeDefinition clothesAttributeDefinition = getDefinition(definitionId);

    if (request.name() != null && !clothesAttributeDefinition.getName().equals(request.name())) {
      checkDuplicateName(request.name());
      clothesAttributeDefinition.update(request.name());
    }

    if (request.selectableValues() != null) {
      List<String> requestValues = request.selectableValues();
      checkDuplicateValue(requestValues);

      List<ClothesAttributeValue> current = clothesAttributeDefinition.getValues();

      // 현재 값(문자열) 리스트
      List<String> currentValues = current.stream()
          .map(ClothesAttributeValue::getValue)
          .toList(); // JDK11이면 Collectors.toList()

      // ★ 순서까지 같으면 스킵 (변경 없음)
      if (!currentValues.equals(requestValues)) {
        // 기존 값 Map (value -> entity)
        Map<String, ClothesAttributeValue> curMap = current.stream()
            .collect(Collectors.toMap(ClothesAttributeValue::getValue, v -> v));

        // 요청 순서대로 재구성 (기존 재사용, 없으면 생성)
        List<ClothesAttributeValue> newValues = new ArrayList<>();
        for (String v : requestValues) {
          ClothesAttributeValue reused = curMap.get(v);
          if (reused != null) {
            newValues.add(reused);
          } else {
            newValues.add(new ClothesAttributeValue(v));
          }
        }

        current.clear();
        for (ClothesAttributeValue val : newValues) {
          if (val.getDefinition() == null) {
            clothesAttributeDefinition.addValue(val);
          } else {
            current.add(val);
          }
        }
      }
    }

    ClothesAttributeDefinition saved = clothesAttributeDefRepository.save(
        clothesAttributeDefinition);
    log.info("의상 속성 수정 완료 - id: {}, name: {}, values: {}", saved.getId(), saved.getName(),
        saved.getValues());

    eventPublisher.publishEvent(new ClothesAttributeEvent("UPDATE", saved));

    return clothesAttributeDefMapper.toDto(saved);
  }

  @Override
  @Transactional
  public void delete(UUID definitionId) {
    ClothesAttributeDefinition clothesAttributeDefinition = getDefinition(definitionId);
    clothesAttributeDefRepository.delete(clothesAttributeDefinition);
    log.info("의상 속성 삭제 완료 - id: {}", definitionId);
  }

  private ClothesAttributeDefinition getDefinition(UUID definitionId) {
    return clothesAttributeDefRepository.findById(definitionId)
        .orElseThrow(
            () -> {
              log.warn("해당 속성 정의가 존재하지 않습니다. definitionId = {}", definitionId);
              return new RestException(ErrorCode.ATTRIBUTE_DEFINITION_NOT_FOUND,
                  Map.of("definitionId", definitionId));
            });
  }

  private void checkDuplicateName(String name) {
    if (clothesAttributeDefRepository.existsByName(name)) {
      log.warn("의상 속성 등록 실패 - 이미 존재하는 속성 이름: {}", name);
      throw new RestException(ErrorCode.ATTRIBUTE_DEFINITION_DUPLICATE,
          Map.of("name", name));
    }
  }

  private void checkDuplicateValue(List<String> values) {
    if (values.isEmpty()) {
      return;
    }
    Set<String> uniqueValues = new HashSet<>();
    for (String value : values) {
      if (!uniqueValues.add(value)) {
        log.warn("의상 속성 등록 실패 - 중복된 속성 값 존재: {}", value);
        throw new RestException(ErrorCode.ATTRIBUTE_VALUE_DUPLICATE,
            Map.of("value", value));
      }
    }
  }
}