package com.team1.otvoo.weather.batch;

import com.team1.otvoo.weather.client.WeatherClient;
import com.team1.otvoo.weather.dto.VilageFcstResponse;
import com.team1.otvoo.weather.dto.VilageFcstResponse.FcstItem;
import com.team1.otvoo.weather.entity.*;
import com.team1.otvoo.weather.factory.WeatherForecastFactory;
import com.team1.otvoo.weather.repository.WeatherForecastRepository;
import com.team1.otvoo.weather.util.ForecastParsingUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WeatherForecastProcessorTest {

  @Mock
  private WeatherClient weatherClient;

  @Mock
  private WeatherForecastRepository weatherForecastRepository;

  private WeatherForecastFactory factory;

  @InjectMocks
  private WeatherForecastProcessor processor;

  @BeforeEach
  void setUp() {
    factory = new WeatherForecastFactory(new ForecastParsingUtils());
    processor = new WeatherForecastProcessor(weatherClient, factory, weatherForecastRepository);
  }

  @Test
  void process_withNullResponse_returnsEmptyList() {
    // given
    WeatherLocation location = new WeatherLocation(60, 127, 37.5, 127.0, List.of("서울"));
    given(weatherClient.getForecast(any(), any(), anyInt(), anyInt()))
        .willReturn(null);

    // when
    List<WeatherForecast> result = processor.process(location);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  void process_withEmptyItems_returnsEmptyList() {
    // given
    WeatherLocation location = new WeatherLocation(60, 127, 37.5, 127.0, List.of("서울"));

    VilageFcstResponse response = new VilageFcstResponse();
    VilageFcstResponse.ResponseBody body = new VilageFcstResponse.ResponseBody();
    VilageFcstResponse.Items items = new VilageFcstResponse.Items();
    items.setItem(Collections.emptyList());
    body.setItems(items);

    VilageFcstResponse.Response res = new VilageFcstResponse.Response();
    res.setBody(body);
    response.setResponse(res);

    given(weatherClient.getForecast(any(), any(), anyInt(), anyInt()))
        .willReturn(response);

    // when
    List<WeatherForecast> result = processor.process(location);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  void process_withValidItems_createsForecasts() {
    // given
    WeatherLocation location = new WeatherLocation(60, 127, 37.5, 127.0, List.of("서울"));

    FcstItem tmpItem = new FcstItem(
        "20250816", "2300",
        "20250817", "0000",
        "TMP", "25",
        60, 127
    );
    FcstItem skyItem = new FcstItem(
        "20250816", "2300",
        "20250817", "0000",
        "SKY", "1",
        60, 127
    );

    VilageFcstResponse response = new VilageFcstResponse();
    VilageFcstResponse.ResponseBody body = new VilageFcstResponse.ResponseBody();
    VilageFcstResponse.Items items = new VilageFcstResponse.Items();
    items.setItem(List.of(tmpItem, skyItem));
    body.setItems(items);

    VilageFcstResponse.Response res = new VilageFcstResponse.Response();
    res.setBody(body);
    response.setResponse(res);

    given(weatherClient.getForecast(any(), any(), anyInt(), anyInt()))
        .willReturn(response);

    given(weatherForecastRepository.existsByLocationAndForecastAtAndForecastedAt(any(), any(), any()))
        .willReturn(false);

    // when
    List<WeatherForecast> result = processor.process(location);

    // then
    assertThat(result).hasSize(1);
    WeatherForecast forecast = result.get(0);
    assertThat(forecast.getLocation()).isEqualTo(location);
    assertThat(forecast.getTemperature().getCurrent()).isEqualTo(25.0);
    assertThat(forecast.getSkyStatus()).isEqualTo(SkyStatus.CLEAR);
  }

  @Test
  void process_withDuplicateForecasts_filtersThemOut() {
    // given
    WeatherLocation location = new WeatherLocation(60, 127, 37.5, 127.0, List.of("서울"));

    FcstItem tmpItem = new FcstItem(
        "20250816", "2300",
        "20250817", "0000",
        "TMP", "25",
        60, 127
    );
    FcstItem skyItem = new FcstItem(
        "20250816", "2300",
        "20250817", "0000",
        "SKY", "1",
        60, 127
    );

    VilageFcstResponse response = new VilageFcstResponse();
    VilageFcstResponse.ResponseBody body = new VilageFcstResponse.ResponseBody();
    VilageFcstResponse.Items items = new VilageFcstResponse.Items();
    items.setItem(List.of(tmpItem, skyItem));
    body.setItems(items);

    VilageFcstResponse.Response res = new VilageFcstResponse.Response();
    res.setBody(body);
    response.setResponse(res);

    given(weatherClient.getForecast(any(), any(), anyInt(), anyInt()))
        .willReturn(response);

    // 중복으로 판단
    given(weatherForecastRepository.existsByLocationAndForecastAtAndForecastedAt(any(), any(), any()))
        .willReturn(true);

    // when
    List<WeatherForecast> result = processor.process(location);

    // then
    assertThat(result).isEmpty();
  }
}
