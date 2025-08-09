package com.team1.otvoo.clothes.repository;

import com.team1.otvoo.clothes.entity.ClothesAttributeValue;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothesAttributeValueRepository extends
    JpaRepository<ClothesAttributeValue, UUID> {

  Optional<ClothesAttributeValue> findByDefinitionIdAndValue(UUID definitionId, String value);
}