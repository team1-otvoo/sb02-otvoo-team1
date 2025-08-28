package com.team1.otvoo.clothes.extraction.pipeline;

import com.team1.otvoo.clothes.entity.ClothesAttributeDefinition;
import com.team1.otvoo.clothes.entity.ClothesAttributeValue;
import com.team1.otvoo.clothes.extraction.dto.ClassificationResult;
import com.team1.otvoo.clothes.repository.ClothesAttributeDefRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AttributeClassifier {

  private final ClothesAttributeDefRepository clothesAttributeDefRepository;

  @Transactional(readOnly = true)
  public ClassificationResult classify(Map<String, String> rawAttributes) {
    if (rawAttributes == null || rawAttributes.isEmpty()) {
      return new ClassificationResult(null, null);
    }

    Map<String, ClothesAttributeDefinition> defByName =
        clothesAttributeDefRepository.findAllWithValues().stream()
            .collect(Collectors.toMap(
                ClothesAttributeDefinition::getName,
                Function.identity()
            ));

    Map<String, String> recognized = new HashMap<>();
    Map<String, String> unknowns = new HashMap<>();

    for (Map.Entry<String, String> e : rawAttributes.entrySet()) {
      String rawKey = trim(e.getKey());
      String rawValue = trim(e.getValue());

      if (rawKey == null || rawValue == null) {
        continue;
      }

      ClothesAttributeDefinition definition = defByName.get(rawKey);
      if (definition == null) {
        unknowns.put(rawKey, rawValue);
        continue;
      }

      List<String> selectableValues = definition.getValues().stream()
          .map(ClothesAttributeValue::getValue)
          .toList();

      HashSet<String> allowedValues = new HashSet<>(selectableValues);

      if (!allowedValues.contains(rawValue)) {
        unknowns.put(rawKey, rawValue);
        continue;
      }

      recognized.put(definition.getName(), rawValue);
    }
    return new ClassificationResult(recognized, unknowns);
  }

  private String trim(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
}