package com.team1.otvoo.weather.repository;

import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.entity.WeatherLocation;
import com.team1.otvoo.weather.repository.custom.WeatherForecastRepositoryCustom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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

  Optional<WeatherForecast> findByLocationAndForecastAtAndForecastedAt(
      WeatherLocation location,
      Instant forecastAt,
      Instant forecastedAt
  );

  @Modifying
  @Transactional
  @Query("DELETE FROM WeatherForecast wf WHERE wf.forecastedAt < :threshold")
  void deleteOlderThan(@Param("threshold") Instant threshold);


  @Query("SELECT wf FROM WeatherForecast wf " +
      "WHERE wf.location = :location " +
      "AND wf.forecastedAt = :forecastedAt " +
      "AND FUNCTION('DATE', wf.forecastAt) = :forecastDate")
  List<WeatherForecast> findByLocationAndForecastedAtAndForecastDate(
      @Param("location") WeatherLocation location,
      @Param("forecastedAt") Instant forecastedAt,
      @Param("forecastDate") LocalDate forecastDate
  );

  @Query("SELECT wf FROM WeatherForecast wf "
      + "JOIN FETCH wf.location l "
      + "JOIN FETCH wf.humidity h "
      + "JOIN FETCH wf.precipitation p "
      + "JOIN FETCH wf.temperature t "
      + "JOIN FETCH wf.windSpeed w "
      + "WHERE wf.id = :id ")
  Optional<WeatherForecast> findByIdFetch(UUID id);
}
