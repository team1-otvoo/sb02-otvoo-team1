package com.team1.otvoo.user.entity;

import com.team1.otvoo.weather.entity.WeatherLocation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Profile{
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(length = 10)
  private Gender gender;

  @Column(length = 50)
  private String name;

  @Column
  private LocalDate birth;

  @Column(name = "temperature_sensitivity")
  private Integer temperatureSensitivity;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "profile_image_id", unique = true)
  private ProfileImage profileImage;

  @OneToOne
  @JoinColumn(name = "weather_location_id", unique = true)
  private WeatherLocation location;

  public Profile (String name) {
    this.name = name;
  }
}