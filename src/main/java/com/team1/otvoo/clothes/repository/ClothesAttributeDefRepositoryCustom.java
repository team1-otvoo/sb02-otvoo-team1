package com.team1.otvoo.clothes.repository;

import com.team1.otvoo.clothes.dto.clothesAttributeDef.ClothesAttributeDefSearchCondition;
import com.team1.otvoo.clothes.entity.ClothesAttributeDefinition;
import java.util.List;

public interface ClothesAttributeDefRepositoryCustom {

  List<ClothesAttributeDefinition> searchWithCursor(
      ClothesAttributeDefSearchCondition condition);

  long countWithCondition(ClothesAttributeDefSearchCondition condition);
}