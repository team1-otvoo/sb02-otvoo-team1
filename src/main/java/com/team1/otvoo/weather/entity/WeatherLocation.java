package com.team1.otvoo.weather.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "weather_locations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeatherLocation {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  private double latitude;
  private double longitude;
  private int x;
  private int y;

  @Column(name = "location_names")
  private String locationNames;

  // 1:N 매핑
  @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<WeatherForecast> forecasts = new ArrayList<>();

  public WeatherLocation(int x, int y, double latitude, double longitude, List<String> locationNames) {
    this.x = x;
    this.y = y;
    this.latitude = latitude;
    this.longitude = longitude;
    this.locationNames = String.join(",", locationNames);
  }

}
