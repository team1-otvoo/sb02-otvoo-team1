package com.team1.otvoo.weather.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "weather_locations")
@Getter
@NoArgsConstructor
public class WeatherLocation {

  @Id
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
