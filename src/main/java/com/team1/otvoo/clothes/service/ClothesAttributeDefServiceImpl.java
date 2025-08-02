package com.team1.otvoo.clothes.service;

import com.team1.otvoo.clothes.dto.ClothesAttributeDefCreateRequest;
import com.team1.otvoo.clothes.dto.ClothesAttributeDefDto;
import com.team1.otvoo.clothes.entity.ClothesAttributeDefinition;
import com.team1.otvoo.clothes.entity.ClothesAttributeValue;
import com.team1.otvoo.clothes.mapper.ClothesAttributeDefMapper;
import com.team1.otvoo.clothes.repository.ClothesAttributeDefRepository;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

    if (clothesAttributeDefRepository.existsByName(request.name())) {
      log.warn("의상 속성 등록 실패 - 이미 존재하는 속성 이름: {}", request.name());
      throw new RestException(ErrorCode.ATTRIBUTE_DEFINITION_DUPLICATE,
          Map.of("name", request.name()));
    }
    Set<String> seen = new HashSet<>();
    for (String value : request.selectableValues()) {
      if (!seen.add(value)) {
        log.warn("의상 속성 등록 실패 - 중복된 속성 값 존재: {}", value);
        throw new RestException(ErrorCode.ATTRIBUTE_VALUE_DUPLICATE,
            Map.of("value", value));
      }
    }
    ClothesAttributeDefinition clothesAttributeDefinition = new ClothesAttributeDefinition(
        request.name(), new ArrayList<>());

    request.selectableValues().forEach(value ->
        clothesAttributeDefinition.addValue(new ClothesAttributeValue(value))
    );

    ClothesAttributeDefinition saved = clothesAttributeDefRepository.save(
        clothesAttributeDefinition);

    log.info("의상 속성 등록 완료 - id: {}, name: {}, values: {}", saved.getId(), saved.getName(),
        saved.getValues());

    return clothesAttributeDefMapper.toDto(saved);
  }
}
