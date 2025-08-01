package com.team1.otvoo.weather.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "weather_forecasts")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherForecast {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "forecasted_at", nullable = false)
  private Instant forecastedAt;

  @Column(name = "forecast_at", nullable = false)
  private Instant forecastAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "sky_status", nullable = false)
  private SkyStatus skyStatus;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @OneToOne(mappedBy = "forecast", cascade = CascadeType.ALL)
  private WeatherLocation location;

  @OneToOne(mappedBy = "forecast", cascade = CascadeType.ALL)
  private WeatherTemperature temperature;

  @OneToOne(mappedBy = "forecast", cascade = CascadeType.ALL)
  private WeatherPrecipitation precipitation;

  @OneToOne(mappedBy = "forecast", cascade = CascadeType.ALL)
  private WeatherHumidity humidity;

  @OneToOne(mappedBy = "forecast", cascade = CascadeType.ALL)
  private WeatherWindSpeed windSpeed;

  public void updateSkyStatus(SkyStatus skyStatus) {
    this.skyStatus = skyStatus;
    this.updatedAt = Instant.now();
  }
}
