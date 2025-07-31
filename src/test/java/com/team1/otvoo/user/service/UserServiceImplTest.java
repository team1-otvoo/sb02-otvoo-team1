package com.team1.otvoo.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.user.dto.UserCreateRequest;
import com.team1.otvoo.user.dto.UserDto;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
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

}