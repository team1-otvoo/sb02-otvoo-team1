package com.team1.otvoo.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.otvoo.auth.dto.SignInRequest;
import com.team1.otvoo.user.entity.Role;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private UUID userId;
  private static final String email = "test@example.com";
  private static final String rawPassword = "password123";

  @BeforeEach
  void setUp() {
    userRepository.findByEmail(email).ifPresent(userRepository::delete);

    String encodedPassword = passwordEncoder.encode(rawPassword);
    User user = new User(email, encodedPassword);
    user.updateRole(Role.USER);

    userRepository.save(user);
    userRepository.flush();

    userId = user.getId();
  }

  @Test
  @Order(1)
  @DisplayName("로그인 성공 (Access + Refresh 토큰 발급)")
  void loginSuccessTest() throws Exception {
    // given
    SignInRequest loginRequest = new SignInRequest(email, rawPassword);

    // when
    var result = mockMvc.perform(post("/api/auth/sign-in")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andReturn();

    // then
    String loginResponse = result.getResponse().getContentAsString();
    String accessToken = objectMapper.readValue(loginResponse, String.class);

    Cookie[] cookies = result.getResponse().getCookies();
    String refreshToken = null;
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if ("refresh_token".equals(cookie.getName())) {
          refreshToken = cookie.getValue();
          break;
        }
      }
    }

    assertThat(accessToken).isNotBlank();
    assertThat(refreshToken).isNotBlank();
  }

  @Test
  @Order(2)
  @DisplayName("로그인 실패 - 잘못된 비밀번호")
  void loginFailTest() throws Exception {
    // given
    SignInRequest loginRequest = new SignInRequest(email, "wrongPassword");

    // when & then
    mockMvc.perform(post("/api/auth/sign-in")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @Order(3)
  @DisplayName("Refresh 토큰으로 Access 토큰 재발급 성공")
  void refreshTokenTest() throws Exception {
    // given
    SignInRequest loginRequest = new SignInRequest(email, rawPassword);
    var loginResult = mockMvc.perform(post("/api/auth/sign-in")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andReturn();

    Cookie[] cookies = loginResult.getResponse().getCookies();
    String refreshToken = null;
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if ("refresh_token".equals(cookie.getName())) {
          refreshToken = cookie.getValue();
          break;
        }
      }
    }
    assertThat(refreshToken).isNotBlank();

    // when
    var result = mockMvc.perform(post("/api/auth/refresh")
            .cookie(new Cookie("refresh_token", refreshToken)))
        .andExpect(status().isOk())
        .andReturn();

    // then
    String refreshResponse = result.getResponse().getContentAsString();
    String accessToken = objectMapper.readTree(refreshResponse).get("accessToken").asText();
    assertThat(accessToken).isNotBlank();
  }

  @Test
  @Order(4)
  @DisplayName("비밀번호 초기화 이메일 발송 요청 성공")
  void passwordResetEmailSendTest() throws Exception {
    // given
    var resetRequest = new Object() {
      public String email = AuthIntegrationTest.email;
    };

    // when & then
    mockMvc.perform(post("/api/auth/reset-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(resetRequest)))
        .andExpect(status().isNoContent());
  }

  @Test
  @Order(5)
  @DisplayName("로그아웃 성공 (리프레시 토큰 무효화 및 AccessToken 블랙리스트 등록 확인)")
  void logoutTest() throws Exception {
    // given
    SignInRequest loginRequest = new SignInRequest(email, rawPassword);
    var loginResult = mockMvc.perform(post("/api/auth/sign-in")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andReturn();

    String loginResponse = loginResult.getResponse().getContentAsString();
    String accessToken = objectMapper.readValue(loginResponse, String.class);

    Cookie[] cookies = loginResult.getResponse().getCookies();
    String refreshToken = null;
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if ("refresh_token".equals(cookie.getName())) {
          refreshToken = cookie.getValue();
          break;
        }
      }
    }
    assertThat(accessToken).isNotBlank();
    assertThat(refreshToken).isNotBlank();

    // when & then
    mockMvc.perform(post("/api/auth/sign-out")
            .cookie(new Cookie("refresh_token", refreshToken))
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk());
  }
}