package com.team1.otvoo.weather.batch;

import com.team1.otvoo.config.BatchConfig;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import com.team1.otvoo.weather.client.WeatherClient;
import com.team1.otvoo.weather.dto.VilageFcstResponse;
import com.team1.otvoo.weather.dto.VilageFcstResponse.FcstItem;
import com.team1.otvoo.weather.dto.VilageFcstResponse.Response;
import com.team1.otvoo.weather.dto.VilageFcstResponse.ResponseBody;
import com.team1.otvoo.weather.dto.VilageFcstResponse.Items;
import com.team1.otvoo.weather.entity.*;
import com.team1.otvoo.user.repository.ProfileRepository;
import com.team1.otvoo.weather.repository.WeatherForecastRepository;
import com.team1.otvoo.weather.repository.WeatherLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;


@SpringBootTest
@SpringBatchTest
@Import(BatchConfig.class)
class WeatherForecastStepIntegrationTest {

  @Autowired private JobLauncherTestUtils jobLauncherTestUtils;
  @Autowired private Job weatherForecastJob;

  @Autowired private UserRepository userRepository;
  @Autowired private ProfileRepository profileRepository;
  @Autowired private WeatherLocationRepository weatherLocationRepository;
  @Autowired private WeatherForecastRepository weatherForecastRepository;

  @MockitoBean
  private WeatherClient weatherClient;

  @BeforeEach
  void cleanDb() {
    weatherForecastRepository.deleteAll();
    profileRepository.deleteAll();
    userRepository.deleteAll();
    weatherLocationRepository.deleteAll();
  }

  @BeforeEach
  void bindJob() {
    // @SpringBatchTest가 등록한 JobLauncherTestUtils에 타겟 Job을 연결
    jobLauncherTestUtils.setJob(weatherForecastJob);
  }

  @Test
  void stepIntegrationTest_savesForecastsToDb() throws Exception {
    // given (DB 세팅)
    User user = User.builder()
        .email("test123@example.com")
        .password("password123")
        .build();
    userRepository.save(user);

    WeatherLocation location = new WeatherLocation(60, 127, 37.5, 127.0, List.of("서울시", "강남구"));
    weatherLocationRepository.save(location);

    Profile profile = new Profile("test123", user);
    profile.setLocation(location);
    profileRepository.save(profile);

    // mock API 응답
    String baseDate = LocalDate.now().toString().replace("-", "");      // 오늘 날짜
    String fcstDate = LocalDate.now().plusDays(1).toString().replace("-", ""); // 내일 날짜

    FcstItem item = new FcstItem(
        baseDate, "2300",      // 발표시각
        fcstDate, "0000",      // 예보시각
        "TMP", "25",           // 기온
        60, 127
    );

    Items items = new Items();
    items.setItem(List.of(item));

    ResponseBody body = new ResponseBody();
    body.setItems(items);

    Response response = new Response();
    response.setBody(body);

    VilageFcstResponse mockResponse = new VilageFcstResponse();
    mockResponse.setResponse(response);

    given(weatherClient.getForecast(anyString(), anyString(), anyInt(), anyInt()))
        .willReturn(mockResponse);

    // when (Step 실행)
    JobExecution jobExecution = jobLauncherTestUtils.launchStep("weatherForecastStep");

    // then
    assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");

    List<WeatherForecast> forecasts = weatherForecastRepository.findAll();
    assertThat(forecasts).hasSize(1);
    assertThat(forecasts.get(0).getTemperature().getCurrent()).isEqualTo(25.0);
  }
}
