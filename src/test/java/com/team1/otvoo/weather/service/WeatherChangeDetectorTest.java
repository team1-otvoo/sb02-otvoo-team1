package com.team1.otvoo.weather.service;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team1.otvoo.notification.service.SendNotificationService;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.ProfileRepository;
import com.team1.otvoo.weather.entity.PrecipitationType;
import com.team1.otvoo.weather.entity.SkyStatus;
import com.team1.otvoo.weather.entity.WeatherForecast;
import com.team1.otvoo.weather.entity.WeatherLocation;
import com.team1.otvoo.weather.entity.WeatherPrecipitation;
import com.team1.otvoo.weather.entity.WeatherTemperature;
import com.team1.otvoo.weather.repository.WeatherForecastRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class WeatherChangeDetectorTest {

  @Mock
  private WeatherForecastRepository weatherForecastRepository;

  @Mock
  private ProfileRepository profileRepository;

  @Mock
  private SendNotificationService sendNotificationService;

  @InjectMocks
  private WeatherChangeDetector weatherChangeDetector;

  private WeatherLocation location;
  private User user;
  private Profile profile;
  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    location = new WeatherLocation(60, 127, 37.5665, 126.9780, List.of("서울", "강남구", "A동"));
    user = User.builder().email("test@test.com").password("1234").build();
    profile = new Profile("사용자", user);
  }

  @Test
  void detectTemperatureChange_whenTemperatureDiffExceedsThreshold_shouldSendNotification() {
    // given
    LocalDate tomorrow = LocalDate.now(ZONE).plusDays(1);
    Instant forecastAt = tomorrow.atTime(6, 0).atZone(ZONE).toInstant();

    Instant yesterdayForecastedAt = tomorrow.minusDays(2).atTime(23, 0).atZone(ZONE).toInstant();
    Instant todayForecastedAt = tomorrow.minusDays(1).atTime(23, 0).atZone(ZONE).toInstant();

    WeatherForecast yesterdayFcst = WeatherForecast.of(yesterdayForecastedAt, forecastAt, SkyStatus.CLEAR);
    yesterdayFcst.setTemperature(new WeatherTemperature(yesterdayFcst, 20.0, null, null, null));

    WeatherForecast todayFcst = WeatherForecast.of(todayForecastedAt, forecastAt, SkyStatus.CLEAR);
    todayFcst.setTemperature(new WeatherTemperature(todayFcst, 25.0, null, null, null));

    when(weatherForecastRepository.findByLocationAndForecastedAtAndForecastDate(eq(location), eq(todayForecastedAt), eq(tomorrow)))
        .thenReturn(List.of(todayFcst));
    when(weatherForecastRepository.findByLocationAndForecastedAtAndForecastDate(eq(location), eq(yesterdayForecastedAt), eq(tomorrow)))
        .thenReturn(List.of(yesterdayFcst));

    when(profileRepository.findByLocation(location)).thenReturn(List.of(profile));

    // when
    weatherChangeDetector.detectChanges(location);

    // then
    verify(sendNotificationService, times(1))
        .sendWeatherForecastNotification(eq(user), eq("기온 변화 알림"), contains("℃ 변했습니다."));
  }

  @Test
  void detectPrecipitationChange_whenPrecipitationChangesFromNoneToRain_shouldSendNotification() {
    // given
    LocalDate tomorrow = LocalDate.now(ZONE).plusDays(1);
    Instant forecastAt = tomorrow.atTime(9, 0).atZone(ZONE).toInstant();

    Instant yesterdayForecastedAt = tomorrow.minusDays(2).atTime(23, 0).atZone(ZONE).toInstant();
    Instant todayForecastedAt = tomorrow.minusDays(1).atTime(23, 0).atZone(ZONE).toInstant();

    WeatherForecast yesterdayFcst = WeatherForecast.of(yesterdayForecastedAt, forecastAt, SkyStatus.CLEAR);
    yesterdayFcst.setPrecipitation(new WeatherPrecipitation(yesterdayFcst, PrecipitationType.NONE, null, 0));

    WeatherForecast todayFcst = WeatherForecast.of(todayForecastedAt, forecastAt, SkyStatus.CLEAR);
    todayFcst.setPrecipitation(new WeatherPrecipitation(todayFcst, PrecipitationType.RAIN, 10.0, 80));

    when(weatherForecastRepository.findByLocationAndForecastedAtAndForecastDate(eq(location), eq(todayForecastedAt), eq(tomorrow)))
        .thenReturn(List.of(todayFcst));
    when(weatherForecastRepository.findByLocationAndForecastedAtAndForecastDate(eq(location), eq(yesterdayForecastedAt), eq(tomorrow)))
        .thenReturn(List.of(yesterdayFcst));

    when(profileRepository.findByLocation(location)).thenReturn(List.of(profile));

    // when
    weatherChangeDetector.detectChanges(location);

    // then
    verify(sendNotificationService, times(1))
        .sendWeatherForecastNotification(eq(user), eq("강수 형태 변화 알림"), contains("강수형태"));
  }
}
