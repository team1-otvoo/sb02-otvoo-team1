package com.team1.otvoo.recommendation.repository;

import com.team1.otvoo.recommendation.entity.ClothesAiAttributes;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothesAiAttributesRepository extends JpaRepository<ClothesAiAttributes, UUID> {
  Optional<ClothesAiAttributes> findByClothesId(UUID clothesId);
}
