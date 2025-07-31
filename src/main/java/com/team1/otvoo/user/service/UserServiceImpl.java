package com.team1.otvoo.user.service;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.user.dto.ChangePasswordRequest;
import com.team1.otvoo.user.dto.UserCreateRequest;
import com.team1.otvoo.user.dto.UserDto;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import com.team1.otvoo.user.util.UserMapper;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;

  @Override
  public UserDto createUser(UserCreateRequest userCreateRequest) {

    String name = userCreateRequest.name();
    String email = userCreateRequest.email().toLowerCase();
    String rawPassword = userCreateRequest.password();
    String encodedPassword = passwordEncoder.encode(rawPassword);

    if (userRepository.existsByEmail(email)) {
      log.warn("중복된 이메일로 회원 생성 시도: {}", email);
      throw new RestException(ErrorCode.CONFLICT, Map.of("email", email));
    }

    Profile profile = new Profile(name);
    User user = new User(email, encodedPassword, profile);
    User savedUser = userRepository.save(user);

    UserDto userDto = userMapper.toUserDto(savedUser);

    return userDto;
  }

  @Override
  public void changePassword(UUID userId, ChangePasswordRequest request) {
    User user = userRepository.findById(userId).orElseThrow(
        () -> {
          log.warn("비밀번호 변경 실패 - 유저 없음: userId={}", userId);
          return new RestException(ErrorCode.NOT_FOUND, Map.of("id", userId));
        }
    );

    String newRawPassword = request.password();
    String newEncodedPassword = passwordEncoder.encode(newRawPassword);

    user.changePassword(newEncodedPassword);
  }
}
