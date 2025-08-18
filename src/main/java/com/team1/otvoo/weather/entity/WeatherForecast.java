package com.team1.otvoo.weather.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "weather_forecasts",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"weather_location_id", "forecast_at", "forecasted_at"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeatherForecast {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "forecasted_at", nullable = false)
  private Instant forecastedAt; // 발표 시각 (baseDate+baseTime)

  @Column(name = "forecast_at", nullable = false)
  private Instant forecastAt;  // 실제 예보 시각 (fcstDate+fcstTime)

  @Enumerated(EnumType.STRING)
  @Column(name = "sky_status", nullable = false)
  private SkyStatus skyStatus;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "weather_location_id", nullable = false)
  private WeatherLocation location;

  @OneToOne(mappedBy = "forecast", cascade = CascadeType.ALL)
  private WeatherTemperature temperature;

  @OneToOne(mappedBy = "forecast", cascade = CascadeType.ALL)
  private WeatherPrecipitation precipitation;

  @OneToOne(mappedBy = "forecast", cascade = CascadeType.ALL)
  private WeatherHumidity humidity;

  @OneToOne(mappedBy = "forecast", cascade = CascadeType.ALL)
  private WeatherWindSpeed windSpeed;

  public WeatherForecast(Instant forecastedAt, Instant forecastAt, SkyStatus skyStatus) {
    this.forecastedAt = forecastedAt;
    this.forecastAt = forecastAt;
    this.skyStatus = skyStatus;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public static WeatherForecast of(Instant forecastedAt, Instant forecastAt, SkyStatus skyStatus) {
    return new WeatherForecast(forecastedAt, forecastAt, skyStatus);
  }

  public void updateSkyStatus(SkyStatus skyStatus) {
    this.skyStatus = skyStatus;
    this.updatedAt = Instant.now();
  }

  public void setLocation(WeatherLocation location) {
    this.location = location;
  }

  public void setTemperature(WeatherTemperature temperature) {
    this.temperature = temperature;
  }

  public void setHumidity(WeatherHumidity humidity) {
    this.humidity = humidity;
  }

  public void setPrecipitation(WeatherPrecipitation precipitation) {
    this.precipitation = precipitation;
  }

  public void setWindSpeed(WeatherWindSpeed windSpeed) {
    this.windSpeed = windSpeed;
  }
}
