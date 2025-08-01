package com.team1.otvoo.clothes.service;

import com.team1.otvoo.clothes.dto.ClothesAttributeDefCreateRequest;
import com.team1.otvoo.clothes.dto.ClothesAttributeDefDto;

public interface ClothesAttributeDefService {
  ClothesAttributeDefDto create(ClothesAttributeDefCreateRequest request);
}
