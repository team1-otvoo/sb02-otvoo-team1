package com.team1.otvoo.user.service;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.user.dto.ChangePasswordRequest;
import com.team1.otvoo.user.dto.UserCreateRequest;
import com.team1.otvoo.user.dto.UserDto;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public UserDto createUser(UserCreateRequest userCreateRequest) {

    String name = userCreateRequest.name();
    String email = userCreateRequest.email().toLowerCase();
    String rawPassword = userCreateRequest.password();
    String encodedPassword = passwordEncoder.encode(rawPassword);

    if (userRepository.existsByEmail(email)) {
      throw new RestException(ErrorCode.CONFLICT, Map.of("email", email));
    }

    Profile profile = new Profile(name);
    User user = new User(email, encodedPassword, profile);
    User savedUser = userRepository.save(user);

    UserDto userDto = new UserDto(
        savedUser.getId(),
        savedUser.getCreatedAt(),
        email,
        name,
        savedUser.getRole(),
        List.of(),
        savedUser.isLocked()
    );

    return userDto;
  }

  @Override
  public void changePassword(UUID userId, ChangePasswordRequest request) {
    User user = userRepository.findById(userId).orElseThrow(
        () -> new RestException(ErrorCode.NOT_FOUND, Map.of("id", userId))
    );

    String newRawPassword = request.password();
    String newEncodedPassword = passwordEncoder.encode(newRawPassword);

    user.changePassword(newEncodedPassword);
  }
}
