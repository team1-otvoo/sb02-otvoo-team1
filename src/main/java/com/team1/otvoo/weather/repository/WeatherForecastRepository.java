package com.team1.otvoo.weather.repository;

import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.entity.WeatherLocation;
import com.team1.otvoo.weather.repository.custom.WeatherForecastRepositoryCustom;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface WeatherForecastRepository extends JpaRepository<WeatherForecast, UUID>,
    WeatherForecastRepositoryCustom {

  boolean existsByLocationAndForecastAtAndForecastedAt(
      WeatherLocation location,
      Instant forecastAt,
      Instant forecastedAt
  );

  @Modifying
  @Transactional
  @Query("DELETE FROM WeatherForecast wf WHERE wf.forecastedAt < :threshold")
  void deleteOlderThan(@Param("threshold") Instant threshold);

}
