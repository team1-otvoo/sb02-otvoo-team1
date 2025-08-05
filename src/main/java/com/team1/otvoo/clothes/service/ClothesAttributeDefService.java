package com.team1.otvoo.clothes.service;

import com.team1.otvoo.clothes.dto.ClothesAttributeDefCreateRequest;
import com.team1.otvoo.clothes.dto.ClothesAttributeDefDto;
import com.team1.otvoo.clothes.dto.ClothesAttributeDefUpdateRequest;
import java.util.UUID;

public interface ClothesAttributeDefService {

  ClothesAttributeDefDto create(ClothesAttributeDefCreateRequest request);

  ClothesAttributeDefDto update(UUID definitionId, ClothesAttributeDefUpdateRequest request);

  void delete(UUID definitionId);
}
