package com.team1.otvoo.weather.repository;

import com.team1.otvoo.weather.entity.WeatherLocation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeatherLocationRepository extends JpaRepository<WeatherLocation, UUID> {
  @Query("SELECT wl FROM WeatherLocation wl WHERE wl.x = :x AND wl.y = :y")
  Optional<WeatherLocation> findByXAndY(@Param("x") int x, @Param("y") int y);
}
