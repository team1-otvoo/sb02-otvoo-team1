package com.team1.otvoo.clothes.repository;

import com.team1.otvoo.clothes.entity.ClothesImage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothesImageRepository extends JpaRepository<ClothesImage, UUID> {

}