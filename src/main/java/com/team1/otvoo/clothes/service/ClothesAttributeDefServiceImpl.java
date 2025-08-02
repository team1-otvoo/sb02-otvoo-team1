package com.team1.otvoo.clothes.service;

import com.team1.otvoo.clothes.dto.ClothesAttributeDefCreateRequest;
import com.team1.otvoo.clothes.dto.ClothesAttributeDefDto;
import com.team1.otvoo.clothes.dto.ClothesAttributeDefUpdateRequest;
import com.team1.otvoo.clothes.entity.ClothesAttributeDefinition;
import com.team1.otvoo.clothes.entity.ClothesAttributeValue;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesAttributeDefServiceImpl implements ClothesAttributeDefService {

  private final ClothesAttributeDefRepository clothesAttributeDefRepository;
  private final ClothesAttributeDefMapper clothesAttributeDefMapper;

  @Override
  @Transactional
  public ClothesAttributeDefDto create(ClothesAttributeDefCreateRequest request) {
    log.info("의상 속성 등록 요청: name = {}, selectableValues = {}",
        request.name(), request.selectableValues());

    checkDuplicateName(request.name());
    List<String> requestValues = request.selectableValues();
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

    return clothesAttributeDefMapper.toDto(saved);
  }

  @Override
  @Transactional
  public ClothesAttributeDefDto update(UUID definitionId,
      ClothesAttributeDefUpdateRequest request) {
    log.info("의상 속성 수정 요청: id = {}, name = {}, selectableValues = {}",
        definitionId, request.name(), request.selectableValues());

    ClothesAttributeDefinition clothesAttributeDefinition = getDefinition(definitionId);

    if (!clothesAttributeDefinition.getName().equals(request.name())) {
      checkDuplicateName(request.name());
      clothesAttributeDefinition.update(request.name());
    }

    List<String> requestValues = request.selectableValues();
    checkDuplicateValue(requestValues);

    List<ClothesAttributeValue> existingValues = clothesAttributeDefinition.getValues();

    if (requestValues.isEmpty()) {
      if (!existingValues.isEmpty()) {
        log.info("의상 속성 수정 - 모든 속성값 삭제");
        existingValues.clear();
      }
    } else {
      // 기존 값 Map (value -> entity) 로 만들어 재사용
      Map<String, ClothesAttributeValue> existingMap = existingValues.stream()
          .collect(Collectors.toMap(ClothesAttributeValue::getValue, v -> v));

      // 요청 리스트대로 새 리스트 구성
      List<ClothesAttributeValue> newValues = new ArrayList<>();
      for (String value : requestValues) {
        ClothesAttributeValue existValue = existingMap.get(value);
        if (existValue != null) {
          newValues.add(existValue); // 기존 value entity 재사용
        } else {
          newValues.add(new ClothesAttributeValue(value)); // 새로운 value entity 추가
        }
      }
      existingValues.clear();
      existingValues.addAll(newValues);
    }

    ClothesAttributeDefinition saved = clothesAttributeDefRepository.save(
        clothesAttributeDefinition);
    log.info("의상 속성 수정 완료 - id: {}, name: {}, values: {}", saved.getId(), saved.getName(),
        saved.getValues());

    return clothesAttributeDefMapper.toDto(saved);
  }

  @Override
  @Transactional
  public void delete(UUID definitionId) {
    ClothesAttributeDefinition clothesAttributeDefinition = getDefinition(definitionId);
    clothesAttributeDefRepository.delete(clothesAttributeDefinition);
    log.info("의상 속성 삭제 완료 - id: {}", definitionId);
  }

  private void checkDuplicateName(String name) {
    if (clothesAttributeDefRepository.existsByName(name)) {
      log.warn("의상 속성 등록 실패 - 이미 존재하는 속성 이름: {}", name);
      throw new RestException(ErrorCode.ATTRIBUTE_DEFINITION_DUPLICATE,
          Map.of("name", name));
    }
  }

  private ClothesAttributeDefinition getDefinition(UUID definitionId) {
    return clothesAttributeDefRepository.findById(definitionId)
        .orElseThrow(
            () -> {
              log.warn("해당 속성 정의가 존재하지 않습니다. definitionId = {}", definitionId);
              return new RestException(ErrorCode.NOT_FOUND, Map.of("definitionId", definitionId));
            });
  }

  private void checkDuplicateValue(List<String> values) {
    if (values == null || values.isEmpty()) {
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
