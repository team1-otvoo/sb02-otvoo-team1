package com.team1.otvoo.weather.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "weather_temperatures")
@Getter
@NoArgsConstructor
public class WeatherTemperature {

  @Id
  @Column(name = "forecast_id")
  private UUID forecastId;

  @MapsId
  @OneToOne
  @JoinColumn(name = "forecast_id")
  private WeatherForecast forecast;

  private double current;
  private Double min;
  private Double max;

  @Column(name = "compared_to_day_before", nullable = false)
  private double comparedToDayBefore;

  public WeatherTemperature(WeatherForecast forecast, double current, Double min, Double max, double comparedToDayBefore) {
    this.forecast = forecast;
    this.forecastId = forecast.getId();
    this.current = current;
    this.min = min;
    this.max = max;
    this.comparedToDayBefore = comparedToDayBefore;
  }
}
