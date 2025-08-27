package com.team1.otvoo.user.entity;

import com.team1.otvoo.user.dto.ProfileUpdateRequest;
import com.team1.otvoo.weather.entity.WeatherLocation;
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
  @JoinColumn(name = "user_id", unique = true)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinColumn(name = "weather_location_id")
  private WeatherLocation location;

  public Profile (String name, User user) {
    this.name = name;
    this.user = user;
  }

  public void setLocation(WeatherLocation location) {
    this.location = location;
  }

  public Profile updateProfile(ProfileUpdateRequest request) {
    if (request.name() != null && !request.name().equals(this.name)) {
      this.name = request.name();
    }

    if (request.gender() != null && !request.gender().equals(this.gender)) {
      this.gender = request.gender();
    }

    if (request.birthDate() != null && !request.birthDate().equals(this.birth)) {
      this.birth = request.birthDate();
    }

    if (request.temperatureSensitivity() != null && !request.temperatureSensitivity().equals(this.temperatureSensitivity)) {
      this.temperatureSensitivity = request.temperatureSensitivity();
    }

    return this;
  }
}
