package com.team1.otvoo.weather.batch;

import com.team1.otvoo.user.repository.ProfileRepository;
import com.team1.otvoo.weather.entity.WeatherLocation;
import com.team1.otvoo.weather.repository.WeatherLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherLocationReaderTest {

  @Mock
  private ProfileRepository profileRepository;

  @Mock
  private WeatherLocationRepository weatherLocationRepository;

  @InjectMocks
  private WeatherLocationReader reader;

  private UUID locationId;
  private WeatherLocation location;

  @BeforeEach
  void setUp() {
    locationId = UUID.randomUUID();
    location = new WeatherLocation(60, 127, 37.5, 127.0, List.of("서울시", "강남구"));
  }

  @Test
  void read_success_returnsLocationsSequentially() {
    // given
    when(profileRepository.findDistinctWeatherLocationIds())
        .thenReturn(List.of(locationId));
    when(weatherLocationRepository.findAllById(List.of(locationId)))
        .thenReturn(List.of(location));

    // when
    WeatherLocation first = reader.read();
    WeatherLocation second = reader.read();

    // then
    assertThat(first).isNotNull();
    assertThat(first.getX()).isEqualTo(60);
    assertThat(second).isNull(); // 두 번째 호출에서는 null
    verify(profileRepository, times(1)).findDistinctWeatherLocationIds();
    verify(weatherLocationRepository, times(1)).findAllById(List.of(locationId));
  }
}
