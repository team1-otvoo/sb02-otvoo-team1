package com.team1.otvoo.user.service;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.user.dto.ChangePasswordRequest;
import com.team1.otvoo.user.dto.UserCreateRequest;
import com.team1.otvoo.user.dto.UserDto;
import com.team1.otvoo.user.dto.UserDtoCursorRequest;
import com.team1.otvoo.user.dto.UserDtoCursorResponse;
import com.team1.otvoo.user.dto.UserSlice;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.projection.UserNameView;
import com.team1.otvoo.user.repository.ProfileRepository;
import com.team1.otvoo.user.repository.UserRepository;
import com.team1.otvoo.user.mapper.UserMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
  private final ProfileRepository profileRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;

  @Transactional(readOnly = true)
  @Override
  public UserDtoCursorResponse getUsers(UserDtoCursorRequest request) {

    UserSlice slice = userRepository.searchUsersWithCursor(request);
    List<User> users = slice.content();


    List<UUID> userIds = users.stream().map(User::getId).toList();
    Map<UUID, String> nameMap = profileRepository.findUserNamesByUserIds(userIds).stream()
        .collect(Collectors.toMap(UserNameView::getUserId, UserNameView::getName));


    List<UserDto> dtos = users.stream()
        .map(user -> {
          String name = nameMap.get(user.getId());
          return userMapper.toUserDto(user, name);
        })
        .toList();

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (slice.hasNext()) {
      User last = users.get(users.size() - 1);
      nextIdAfter = last.getId();
      nextCursor = switch (request.sortBy()) {
        case EMAIL -> last.getEmail();
        case CREATED_AT -> last.getCreatedAt().toString();
      };
    }

    return new UserDtoCursorResponse(
        dtos,
        nextCursor,
        nextIdAfter,
        slice.hasNext(),
        slice.totalCount(),
        request.sortBy(),
        request.sortDirection()
    );
  }

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

    User user = new User(email, encodedPassword);
    Profile profile = new Profile(name, user);
    User savedUser = userRepository.save(user);
    profileRepository.save(profile);

    UserDto userDto = userMapper.toUserDto(savedUser, name);

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
