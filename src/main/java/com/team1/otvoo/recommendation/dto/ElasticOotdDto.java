package com.team1.otvoo.recommendation.dto;

import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeWithDefDto;
import com.team1.otvoo.clothes.entity.ClothesType;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@Builder
public class ElasticOotdDto {
  UUID clothesId;
  String name;
  String imageKey;
  String contentType;
  ClothesType type;
  List<ClothesAttributeWithDefDto> attributes;
}
