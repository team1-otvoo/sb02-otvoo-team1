package com.team1.otvoo.clothes.repository;

import com.team1.otvoo.clothes.dto.ClothesSearchCondition;
import com.team1.otvoo.clothes.entity.Clothes;
import java.util.List;

public interface ClothesRepositoryCustom {

  List<Clothes> searchWithCursor(ClothesSearchCondition condition);

  long countWithCondition(ClothesSearchCondition condition);
}
