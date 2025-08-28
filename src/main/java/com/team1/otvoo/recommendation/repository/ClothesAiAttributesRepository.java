package com.team1.otvoo.recommendation.repository;

import com.team1.otvoo.clothes.entity.ClothesType;
import com.team1.otvoo.recommendation.entity.ClothesAiAttributes;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClothesAiAttributesRepository extends JpaRepository<ClothesAiAttributes, UUID> {
  Optional<ClothesAiAttributes> findByClothesId(UUID clothesId);


  @Query("""
    select distinct ca
    from ClothesAiAttributes ca
    left join fetch ca.clothes c
    where c.owner.id = :userId
    """)
  List<ClothesAiAttributes> findByUserIdClothes_TypeInFetch(@Param("userId") UUID userId);
}
