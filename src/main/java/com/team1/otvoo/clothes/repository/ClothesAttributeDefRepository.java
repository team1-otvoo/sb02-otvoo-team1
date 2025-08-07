package com.team1.otvoo.clothes.repository;

import com.team1.otvoo.clothes.entity.ClothesAttributeDefinition;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothesAttributeDefRepository extends JpaRepository<ClothesAttributeDefinition, UUID>, ClothesAttributeDefRepositoryCustom {

  boolean existsByName(String name);
}
