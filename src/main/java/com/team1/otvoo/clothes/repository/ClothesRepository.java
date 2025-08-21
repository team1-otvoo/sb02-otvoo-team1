package com.team1.otvoo.clothes.repository;

import com.team1.otvoo.clothes.entity.Clothes;
import com.team1.otvoo.clothes.entity.ClothesType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClothesRepository extends JpaRepository<Clothes, UUID>, ClothesRepositoryCustom {
  @Query("""
    select distinct c
    from Clothes c
    left join fetch c.selectedValues sv
    left join fetch sv.definition d
    left join fetch sv.value v
    where c.owner.id = :userId
    """)
  List<Clothes> findByUserIdFetch(@Param("userId") UUID userId);

  @Query("""
    select distinct c
    from Clothes c
    left join fetch c.selectedValues sv
    left join fetch sv.definition d
    left join fetch sv.value v
    where c.owner.id = :userId and c.id in :clothesIds
    """)
  List<Clothes> findByIdInWithDetails(@Param("clothesIds") List<UUID> clothesIds, @Param("userId") UUID userId);
}
