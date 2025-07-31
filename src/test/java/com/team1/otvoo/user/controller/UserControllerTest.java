package com.team1.otvoo.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.user.dto.UserCreateRequest;
import com.team1.otvoo.user.dto.UserDto;
import com.team1.otvoo.user.entity.Role;
import com.team1.otvoo.user.service.UserService;
import jakarta.annotation.Resource;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(UserController.class)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserService userService;

  @Resource
  private ObjectMapper objectMapper;

  @Test
  @DisplayName("회원 가입 성공")
  void createUser_return201() throws Exception {
    UserCreateRequest request = new UserCreateRequest(
        "홍길동",
        "test@example.com",
        "password123!"
    );
    UserDto response = new UserDto(
        UUID.randomUUID(),
        Instant.now(),
        "test@example.com",
        "홍길동",
        Role.USER,
        List.of(),
        false
    );

    given(userService.createUser(any())).willReturn(response);

    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {
                        "name": "홍길동",
                        "email": "test@example.com",
                        "password": "password123!"
                    }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("홍길동"))
        .andExpect(jsonPath("$.email").value("test@example.com"));
  }

  @Test
  @DisplayName("유저 생성시 유효성 검증 실패")
  void createUser_invalidArgument() throws Exception {
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {
                        "name": "",
                        "email": "invalid-email",
                        "password": "short"
                    }
                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("중복 이메일일 경우 409 CONFLICT 응답을 반환한다")
  void createUser_withDuplicateEmail_shouldReturnConflict() throws Exception {
    // given
    UserCreateRequest request = new UserCreateRequest("홍길동", "test@example.com", "password123!");

    Mockito.doThrow(new com.team1.otvoo.exception.RestException(
        ErrorCode.CONFLICT,
        Map.of("email", "test@example.com")
    )).when(userService).createUser(any(UserCreateRequest.class));

    // when & then
    mockMvc.perform(post("/api/users") // 실제 API 경로에 맞게 수정하세요
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.exceptionName").value("CONFLICT"))
        .andExpect(jsonPath("$.message").value("이미 존재하는 리소스입니다."))
        .andExpect(jsonPath("$.details.email").value("test@example.com"));
  }
}
