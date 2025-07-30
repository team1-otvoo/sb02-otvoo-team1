package com.team1.otvoo.weather.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "weather_wind_speeds")
@Getter
@NoArgsConstructor
public class WeatherWindSpeed {
  @Id
  @Column(name = "forecast_id")
  private UUID forecastId;

  @MapsId
  @OneToOne
  @JoinColumn(name = "forecast_id")
  private WeatherForecast forecast;

  @Column(nullable = false)
  private double speed;

  @Enumerated(EnumType.STRING)
  @Column(name = "as_word", nullable = false)
  private WindStrength asWord;

}
