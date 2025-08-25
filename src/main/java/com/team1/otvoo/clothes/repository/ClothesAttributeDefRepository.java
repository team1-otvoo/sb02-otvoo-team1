package com.team1.otvoo.clothes.repository;

import com.team1.otvoo.clothes.entity.ClothesAttributeDefinition;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClothesAttributeDefRepository extends JpaRepository<ClothesAttributeDefinition, UUID>, ClothesAttributeDefRepositoryCustom {

  boolean existsByName(String name);

  @Query("""
      select distinct d
            from ClothesAttributeDefinition  d
                  left join fetch d.values v
                        order by d.name asc
      """)
  List<ClothesAttributeDefinition> findAllWithValues();
}
