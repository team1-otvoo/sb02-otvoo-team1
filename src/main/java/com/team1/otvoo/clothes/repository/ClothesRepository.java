package com.team1.otvoo.clothes.repository;

import com.team1.otvoo.clothes.entity.Clothes;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothesRepository extends JpaRepository<Clothes, UUID>, ClothesRepositoryCustom {

}