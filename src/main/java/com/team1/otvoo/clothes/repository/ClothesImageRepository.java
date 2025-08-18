package com.team1.otvoo.clothes.repository;

import com.team1.otvoo.clothes.entity.ClothesImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothesImageRepository extends JpaRepository<ClothesImage, UUID> {

  List<ClothesImage> findAllByClothes_IdIn(List<UUID> clothesIds);

  Optional<ClothesImage> findByClothes_Id(UUID clothesId);
}