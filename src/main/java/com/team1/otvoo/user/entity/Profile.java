package com.team1.otvoo.user.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;

@Entity
@Table(name = "profiles")
@Getter
public class Profile{
  @Id
  private UUID id;

  @Column(length = 10)
  private Gender gender;

  @Enumerated(EnumType.STRING)
  @Column(length = 50)
  private String name;

  @Column
  private LocalDate birth;

  @Column(name = "temperature_sensitivity")
  private Integer temperatureSensitivity;

  @OneToOne
  @JoinColumn(name = "user_id", unique = true)
  private User user;

  @OneToOne(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private ProfileImage profileImage;

  @OneToOne
  @JoinColumn(name = "weather_location_id", unique = true)
  private WeatherLocation location;
}