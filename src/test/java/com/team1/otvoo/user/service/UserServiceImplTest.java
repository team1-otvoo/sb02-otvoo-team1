package com.team1.otvoo.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.user.dto.ChangePasswordRequest;
import com.team1.otvoo.user.dto.UserCreateRequest;
import com.team1.otvoo.user.dto.UserDto;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @InjectMocks
  private UserServiceImpl userService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  private UserCreateRequest request;

  @BeforeEach
  void setUp() {
    request =new UserCreateRequest(
        "testUser1",
        "testUser1@email.com",
        "password1234!"
    );
  }

  @Test
  @DisplayName("이메일 중복 시 예외 던짐")
  void createUser_with_duplicate_email() {
    given(userRepository.existsByEmail("testuser1@email.com")).willReturn(true);

    assertThrows(RestException.class, () -> userService.createUser(request));
  }

  @Test
  @DisplayName("회원 가입 성공")
  void createUser_success() {
    given(userRepository.existsByEmail("testuser1@email.com")).willReturn(false);
    given(passwordEncoder.encode(anyString())).willReturn("encodedPassword1234!");
    given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

    UserDto result = userService.createUser(request);

    assertEquals("testUser1", result.name());
    assertEquals("testuser1@email.com", result.email());
  }

  @Test
  @DisplayName("비밀번호 인코딩")
  void createUser_encoded_password() {
    given(userRepository.existsByEmail(anyString())).willReturn(false);
    given(passwordEncoder.encode("password1234!")).willReturn("encodedPassword");
    given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

    userService.createUser(request);

    verify(passwordEncoder).encode("password1234!");
  }

  @Test
  @DisplayName("비밀번호 변경 성공")
  void changePassword_success() {
    // given
    UUID userId = UUID.randomUUID();
    String rawPassword = "newPassword123!";
    String encodedPassword = "encodedPassword123!";
    ChangePasswordRequest request = new ChangePasswordRequest(rawPassword);
    User user = mock(User.class);     // should() 를 사용하기 위해서 (실제로 changePassword() 가 사용되었는지 추적하기 위해)

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(passwordEncoder.encode(rawPassword)).willReturn(encodedPassword);

    // when
    userService.changePassword(userId, request);

    // then
    then(user).should().changePassword(encodedPassword);
  }

  @Test
  @DisplayName("존재하지 않는 사용자 ID일 경우 예외 발생")
  void changePassword_userNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    ChangePasswordRequest request = new ChangePasswordRequest("newPassword123!");

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.changePassword(userId, request))
        .isInstanceOf(RestException.class)
        .hasMessage(ErrorCode.NOT_FOUND.getMessage());

    then(userRepository).shouldHaveNoMoreInteractions();
  }

  @Test
  @DisplayName("기존 비밀번호와 동일하면 예외 발생")
  void changePassword_samePassword() {
    // given
    UUID userId = UUID.randomUUID();
    String rawPassword = "password123!";
    String encodedPassword = "encodedPassword123!";
    ChangePasswordRequest request = new ChangePasswordRequest(rawPassword);
    User user = mock(User.class);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(passwordEncoder.encode(rawPassword)).willReturn(encodedPassword);

    willThrow(new RestException(ErrorCode.SAME_AS_OLD_PASSWORD))
        .given(user)
        .changePassword(encodedPassword);

    // when & then
    assertThatThrownBy(() -> userService.changePassword(userId, request))
        .isInstanceOf(RestException.class)
        .hasMessage(ErrorCode.SAME_AS_OLD_PASSWORD.getMessage());

    then(user).should().changePassword(encodedPassword);
  }
}