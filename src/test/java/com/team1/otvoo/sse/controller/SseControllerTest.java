package com.team1.otvoo.sse.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team1.otvoo.auth.token.AccessTokenStore;
import com.team1.otvoo.security.CustomUserDetails;
import com.team1.otvoo.security.CustomUserDetailsService;
import com.team1.otvoo.security.JwtTokenProvider;
import com.team1.otvoo.sse.service.SseServiceImpl;
import com.team1.otvoo.user.entity.User;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@WebMvcTest(SseController.class)
class SseControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private SseServiceImpl sseService;

  @MockitoBean
  private JwtTokenProvider jwtTokenProvider;
  @MockitoBean
  private CustomUserDetailsService customUserDetailsService;
  @MockitoBean
  private AccessTokenStore accessTokenStore;

  private CustomUserDetails userDetails;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();

    User user = new User("test@email.com", "password");
    ReflectionTestUtils.setField(user, "id", userId);

    userDetails = new CustomUserDetails(user);
  }

  @Test
  @DisplayName("SSE 연결_성공")
  void subscribe_Success_ShouldReturnSseEmitter() throws Exception {
    // given
    UUID lastEventId = UUID.randomUUID();
    SseEmitter emitter = new SseEmitter(60_000L);
    emitter.send(SseEmitter.event().name("notificationEvent").data("notificationInfo"));

    given(sseService.connect(eq(userId), eq(lastEventId))).willReturn(emitter);

    // when & then
    mockMvc.perform(get("/api/sse")
            .with(authentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())))
            .param("LastEventId", lastEventId.toString())
            .accept(MediaType.TEXT_EVENT_STREAM))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM_VALUE));

    then(sseService).should().connect(userId, lastEventId);
  }
}
