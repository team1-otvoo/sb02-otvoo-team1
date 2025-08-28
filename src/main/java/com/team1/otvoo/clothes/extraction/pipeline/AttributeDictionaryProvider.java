package com.team1.otvoo.clothes.extraction.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.team1.otvoo.clothes.entity.ClothesAttributeDefinition;
import com.team1.otvoo.clothes.entity.ClothesAttributeValue;
import com.team1.otvoo.clothes.repository.ClothesAttributeDefRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttributeDictionaryProvider {

  private final ClothesAttributeDefRepository clothesAttributeDefRepository;
  private final ObjectMapper objectMapper;

  @Transactional(readOnly = true)
  public String exportAttributeData() {
    Map<String, List<String>> dictionary = getDictionary();
    return toJson(dictionary);
  }

  @Transactional(readOnly = true)
  public Map<String, List<String>> getDictionary() {
    List<ClothesAttributeDefinition> definitions = clothesAttributeDefRepository.findAllWithValues();

    Map<String, List<String>> dictionary = new LinkedHashMap<>();
    for (ClothesAttributeDefinition definition : definitions) {
      String key = definition.getName();
      List<String> values = definition.getValues().stream()
          .map(ClothesAttributeValue::getValue)
          .toList();

      dictionary.put(key, values);
    }
    return dictionary;
  }

  private String toJson(Map<String, List<String>> dictionary) {
    ObjectNode root = objectMapper.createObjectNode();
    ObjectNode attributes = root.putObject("attributes");
    dictionary.forEach((key, values) -> {
      ArrayNode arrayNode = attributes.putArray(key);
      values.forEach(arrayNode::add);
    });
    try {
      return objectMapper.writeValueAsString(root);
    } catch (JsonProcessingException e) {
      log.warn("Failed to serialize known attributes json", e);
      return "{\"attributes\":{}}";
    }
  }
}
