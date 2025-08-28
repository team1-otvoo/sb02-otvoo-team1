package com.team1.otvoo.weather.batch;

import com.team1.otvoo.user.repository.ProfileRepository;
import com.team1.otvoo.weather.entity.WeatherLocation;
import com.team1.otvoo.weather.repository.WeatherLocationRepository;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class WeatherLocationReader implements ItemReader<WeatherLocation> {

  private final ProfileRepository profileRepository;
  private final WeatherLocationRepository weatherLocationRepository;

  private Iterator<WeatherLocation> iterator;

  @Override
  public WeatherLocation read() {
    if (iterator == null) {
      List<UUID> locationIds = profileRepository.findDistinctWeatherLocationIds();
      List<WeatherLocation> locations = weatherLocationRepository.findAllById(locationIds);
      iterator = locations.iterator(); // iterator로 변환 후 순차적으로 꺼낼 수 있도록 준비
    }
    return iterator.hasNext() ? iterator.next() : null;
  }
}
