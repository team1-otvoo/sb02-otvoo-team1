package com.team1.otvoo.weather.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "weather_locations")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherLocation {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne
  @JoinColumn(name = "forecast_id", nullable = false)
  private WeatherForecast forecast;

  private double latitude;
  private double longitude;
  private int x;
  private int y;

  @Column(name = "location_names")
  private String locationNames;

  public WeatherLocation(WeatherForecast forecast, int x, int y, double latitude, double longitude, List<String> locationNames) {
    this.forecast = forecast;
    this.x = x;
    this.y = y;
    this.latitude = latitude;
    this.longitude = longitude;
    this.locationNames = String.join(",", locationNames);
  }

  public void updateCoordinates(double latitude, double longitude, int x, int y) {
    this.latitude = latitude;
    this.longitude = longitude;
    this.x = x;
    this.y = y;
  }

  public void updateLocationNames(String locationNames) {
    this.locationNames = locationNames;
  }
}
