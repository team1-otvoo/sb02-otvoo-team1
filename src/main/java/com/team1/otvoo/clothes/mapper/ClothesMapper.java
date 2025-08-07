package com.team1.otvoo.clothes.mapper;

import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeWithDefDto;
import com.team1.otvoo.clothes.entity.Clothes;
import com.team1.otvoo.clothes.dto.OotdDto;
import com.team1.otvoo.clothes.entity.ClothesSelectedValue;
import java.util.Collections;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ClothesMapper {
  ClothesMapper INSTANCE = Mappers.getMapper(ClothesMapper.class);

  @Mapping(source = "id", target = "clothesId")
  @Mapping(source = "selectedValues", target = "attributes")
  OotdDto toOotdDto(Clothes clothes);

  @Mapping(source = "definition.id", target = "definitionId")
  @Mapping(source = "definition.name", target = "definitionName")
  @Mapping(expression = "java(getSelectableValues(selectedValue))", target = "selectableValues")
  @Mapping(source = "value.value", target = "value")
  ClothesAttributeWithDefDto toAttributeDefDto(ClothesSelectedValue selectedValue);

  default List<String> getSelectableValues(ClothesSelectedValue selectedValue) {
    if (selectedValue == null || selectedValue.getDefinition() == null || selectedValue.getDefinition().getValues() == null) {
      return Collections.emptyList();
    }
    return selectedValue.getDefinition().getValues()
        .stream()
        .map(clothesAttributeValue -> clothesAttributeValue.getValue())
        .toList();
  }

  default List<ClothesAttributeWithDefDto> toDtoList(List<ClothesSelectedValue> values) {
    if (values == null) {
      return Collections.emptyList();
    }
    return values.stream()
        .map(this::toAttributeDefDto)
        .distinct() // 중복 제거
        .toList();
  }
}
