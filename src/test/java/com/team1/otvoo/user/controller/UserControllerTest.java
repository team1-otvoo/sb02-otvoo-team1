package com.team1.otvoo.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.user.dto.ChangePasswordRequest;
import com.team1.otvoo.user.dto.Location;
import com.team1.otvoo.user.dto.ProfileDto;
import com.team1.otvoo.user.dto.SortBy;
import com.team1.otvoo.user.dto.SortDirection;
import com.team1.otvoo.user.dto.UserCreateRequest;
import com.team1.otvoo.user.dto.UserDto;
import com.team1.otvoo.user.dto.UserDtoCursorResponse;
import com.team1.otvoo.user.entity.Gender;
import com.team1.otvoo.user.entity.Role;
import com.team1.otvoo.user.service.UserService;
import com.team1.otvoo.weather.entity.WeatherLocation;
import jakarta.annotation.Resource;
import java.time.Instant;
import java.time.LocalDate;
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
  @DisplayName("사용자 목록 조회 성공 시 200 OK")
  void getUserList_success_shouldReturn200() throws Exception {
    // given
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();

    UserDto user1 = new UserDto(
        id1,
        Instant.parse("2024-01-01T10:00:00Z"),
        "alice@example.com",
        "Alice",
        Role.USER,
        List.of(),
        false
    );

    UserDto user2 = new UserDto(
        id2,
        Instant.parse("2024-01-01T11:00:00Z"),
        "bob@example.com",
        "Bob",
        Role.USER,
        List.of(),
        false
    );

    UserDtoCursorResponse response = new UserDtoCursorResponse(
        List.of(user1, user2),
        "cursor123",
        user2.id(),
        true,
        100L,
        SortBy.CREATED_AT,
        SortDirection.DESCENDING
    );

    given(userService.getUsers(any())).willReturn(response);

    // when & then
    mockMvc.perform(get("/api/users")
            .param("limit", "10")
            .param("sortBy", "CREATED_AT")
            .param("sortDirection", "DESCENDING"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0].email").value("alice@example.com"))
        .andExpect(jsonPath("$.data[1].email").value("bob@example.com"))
        .andExpect(jsonPath("$.hasNext").value(true))
        .andExpect(jsonPath("$.totalCount").value(100))
        .andExpect(jsonPath("$.sortBy").value("created_at"))
        .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));
  }

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

    Mockito.doThrow(new RestException(
        ErrorCode.CONFLICT,
        Map.of("email", "test@example.com")
    )).when(userService).createUser(any(UserCreateRequest.class));

    // when & then
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.exceptionName").value("CONFLICT"))
        .andExpect(jsonPath("$.message").value("이미 존재하는 리소스입니다."))
        .andExpect(jsonPath("$.details.email").value("test@example.com"));
  }

  @Test
  @DisplayName("비밀번호 변경 성공 시 204 반환")
  void changePassword_success_shouldReturnNoContent() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    Mockito.doNothing()
        .when(userService)
        .changePassword(Mockito.eq(userId), any(ChangePasswordRequest.class));

    mockMvc.perform(patch("/api/users/{userId}/password", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
              {
                  "password": "newStrongPassword123!"
              }
          """))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("기존과 동일한 비밀번호로 변경 시 400 반환")
  void changePassword_samePassword_shouldReturnBadRequest() throws Exception {
    UUID userId = UUID.randomUUID();

    Mockito.doThrow(new RestException(
        ErrorCode.SAME_AS_OLD_PASSWORD
    )).when(userService).changePassword(Mockito.eq(userId), any(ChangePasswordRequest.class));

    mockMvc.perform(patch("/api/users/{userId}/password", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
              {
                  "password": "password123!"
              }
          """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionName").value("SAME_AS_OLD_PASSWORD"))
        .andExpect(jsonPath("$.message").value("기존 비밀번호와 동일한 비밀번호는 사용할 수 없습니다."));
  }

  @Test
  @DisplayName("프로필 조회 성공 시 200 OK 응답")
  void getUserProfile_success_shouldReturn200() throws Exception {
    // given
    double latitude = 37.5665;
    double longitude = 126.9780;
    int x = 3;
    int y = 4;
    List<String> locationNames = List.of("서울특별시", "강남구", "역삼동");
    Location location = new Location(
        latitude,
        longitude,
        x,
        y,
        locationNames
    );

    UUID userId = UUID.randomUUID();
    String profileImageUrl = "imageUrl";
    ProfileDto dto = new ProfileDto(
        userId,
        "홍길동",
        Gender.MALE,
        LocalDate.of(1990, 1, 1),
        location,
        1,
        profileImageUrl
    );

    given(userService.getUserProfile(Mockito.eq(userId))).willReturn(dto);

    // when & then
    mockMvc.perform(get("/api/users/{userId}/profiles", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId.toString()))
        .andExpect(jsonPath("$.name").value("홍길동"))
        .andExpect(jsonPath("$.gender").value("MALE"))
        .andExpect(jsonPath("$.birthDate").value("1990-01-01"))
        .andExpect(jsonPath("$.temperatureSensitivity").value(1))
        .andExpect(jsonPath("$.profileImageUrl").value("imageUrl"))
        .andExpect(jsonPath("$.location.locationNames[0]").value("서울특별시"));
  }

}
