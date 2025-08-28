package com.team1.otvoo.weather.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.team1.otvoo.weather.client.WeatherClient;
import com.team1.otvoo.weather.dto.VilageFcstResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeatherClientTest {

  @Mock
  private WeatherClient weatherClient;

  @Test
  void getForecast_returnsMockResponse() {
    // given
    VilageFcstResponse.FcstItem item = new VilageFcstResponse.FcstItem(
        "20250828", "2300", "20250829", "0000", "TMP", "25", 60, 127
    );

    VilageFcstResponse.Items items = new VilageFcstResponse.Items();
    items.setItem(List.of(item));

    VilageFcstResponse.ResponseBody body = new VilageFcstResponse.ResponseBody();
    body.setItems(items);

    VilageFcstResponse.Response response = new VilageFcstResponse.Response();
    response.setBody(body);

    VilageFcstResponse mockResponse = new VilageFcstResponse();
    mockResponse.setResponse(response);

    given(weatherClient.getForecast(anyString(), anyString(), anyInt(), anyInt()))
        .willReturn(mockResponse);

    // when
    VilageFcstResponse actual =
        weatherClient.getForecast("20250828", "2300", 60, 127);

    // then
    assertThat(actual.getResponse().getBody().getItems().getItem()).hasSize(1);
    assertThat(actual.getResponse().getBody().getItems().getItem().get(0).getFcstValue())
        .isEqualTo("25");
  }
}
