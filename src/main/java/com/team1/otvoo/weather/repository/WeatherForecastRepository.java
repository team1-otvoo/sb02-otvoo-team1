package com.team1.otvoo.weather.repository;

import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.repository.custom.WeatherForecastRepositoryCustom;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeatherForecastRepository extends JpaRepository<WeatherForecast, UUID>,
    WeatherForecastRepositoryCustom {

}
