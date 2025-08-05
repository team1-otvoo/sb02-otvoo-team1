package com.team1.otvoo.clothes.mapper;

import com.team1.otvoo.clothes.dto.ClothesAttributeDefDto;
import com.team1.otvoo.clothes.entity.ClothesAttributeDefinition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClothesAttributeDefMapper {

  @Mapping(target = "selectableValues",
      expression = "java(definition.getValues().stream()"
          + ".map(v -> v.getValue())"
          + ".toList())")
  ClothesAttributeDefDto toDto(ClothesAttributeDefinition definition);
}
