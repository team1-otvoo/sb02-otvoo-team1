package com.team1.otvoo.user.service;

import com.team1.otvoo.auth.token.AccessTokenStore;
import com.team1.otvoo.auth.token.RefreshTokenStore;
import com.team1.otvoo.auth.token.TemporaryPasswordStore;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.security.JwtTokenProvider;
import com.team1.otvoo.user.dto.ChangePasswordRequest;
import com.team1.otvoo.user.dto.ProfileDto;
import com.team1.otvoo.user.dto.ProfileUpdateRequest;
import com.team1.otvoo.user.dto.UserCreateRequest;
import com.team1.otvoo.user.dto.UserDto;
import com.team1.otvoo.user.dto.UserDtoCursorRequest;
import com.team1.otvoo.user.dto.UserDtoCursorResponse;
import com.team1.otvoo.user.dto.UserLockUpdateRequest;
import com.team1.otvoo.user.dto.UserRoleUpdateRequest;
import com.team1.otvoo.user.dto.UserRow;
import com.team1.otvoo.user.dto.UserSlice;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.mapper.ProfileMapper;
import com.team1.otvoo.user.repository.ProfileImageRepository;
import com.team1.otvoo.user.repository.ProfileRepository;
import com.team1.otvoo.user.repository.UserRepository;
import com.team1.otvoo.user.mapper.UserMapper;
import com.team1.otvoo.user.resolver.ProfileImageUrlResolver;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class UserServiceImpl implements UserService {

  private final ProfileImageService profileImageService;

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;
  private final ProfileImageRepository profileImageRepository;

  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;
  private final ProfileMapper profileMapper;
  private final ProfileImageUrlResolver profileImageUrlResolver;

  private final TemporaryPasswordStore temporaryPasswordStore;
  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenStore refreshTokenStore;
  private final AccessTokenStore accessTokenStore;

  @Transactional(readOnly = true)
  @Override
  public UserDtoCursorResponse getUsers(UserDtoCursorRequest request) {
    UserSlice<UserRow> slice = userRepository.searchUserRowWithCursor(request);
    List<UserRow> rows = slice.content();

    // Row → API DTO 매핑 (이 단계에서 플레이스홀더 처리 등 가능)
    List<UserDto> dtos = rows.stream()
        .map(userRow -> userMapper.toUserDtoFromUserRow(
            userRow,
            List.of()
        ))
        .toList();

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (slice.hasNext()) {
      UserRow last = rows.get(rows.size() - 1);
      nextIdAfter = last.id();
      nextCursor = switch (request.sortBy()) {
        case EMAIL     -> last.email();
        case CREATED_AT-> last.createdAt().toString();
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
  public UserDto updateUserRole(UUID userId, UserRoleUpdateRequest request) {

    Profile profile = profileRepository.findByUserIdWithUser(userId).orElseThrow(
        () -> {
          log.warn("해당 userId를 가진 profile 을 찾을 수 없습니다. - [{}]", userId);
          return new RestException(ErrorCode.NOT_FOUND, Map.of("userId", userId));
        }
    );

    if (!profile.getUser().getRole().equals(request.role())) {
      profile.getUser().updateRole(request.role());

      // 권한 변경시 로그아웃
      refreshTokenStore.remove(userId);

      String accessToken = accessTokenStore.get(userId);
      long expiration = jwtTokenProvider.getExpirationSecondsLeft(accessToken);
      accessTokenStore.blacklistAccessToken(accessToken, expiration);
      accessTokenStore.remove(userId);
    }

    UserDto userDto = userMapper.toUserDto(profile.getUser(), profile.getName());

    return userDto;
  }

  @Transactional(readOnly = true)
  @Override
  public ProfileDto getUserProfile(UUID userId) {

    Profile profile = profileRepository.findByUserId(userId).orElseThrow(
        () -> {
          log.warn("해당 userId를 가진 profile을 찾을 수 없습니다. - [{}]", userId);
          return new RestException(ErrorCode.NOT_FOUND, Map.of("profile-userId", userId));
        }
    );

    UUID profileId = profile.getId();

    String imageUrl = profileImageUrlResolver.resolve(profileId);

    ProfileDto dto = profileMapper.toProfileDto(userId, profile, imageUrl);

    return dto;
  }

  @Override
  public ProfileDto updateProfile(UUID userId, ProfileUpdateRequest profileUpdateRequest,
      MultipartFile profileImageFile) {

    Profile profile = profileRepository.findByUserId(userId).orElseThrow(
        () -> {
          log.warn("해당 userId를 가진 Profile을 찾을 수 없습니다. - [{}]", userId);
          return new RestException(ErrorCode.NOT_FOUND, Map.of("profile-userId", userId));
        }
    );

    profile.updateProfile(profileUpdateRequest);

    String profileImageUrl = profileImageService.replaceProfileImageAndGetUrl(profile, profileImageFile);

    ProfileDto dto = profileMapper.toProfileDto(userId, profile, profileImageUrl);

    return dto;
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
    temporaryPasswordStore.remove(userId);
  }

  @Override
  public UUID changeLock(UUID userId, UserLockUpdateRequest request) {
    User user = userRepository.findById(userId).orElseThrow(
        () -> {
          log.warn("해당 userId를 가진 User 를 찾을 수 없습니다. - [{}]", userId);
          return new RestException(ErrorCode.NOT_FOUND, Map.of("id", userId));
        }
    );

    user.updateLocked(request.locked());

    if (user.isLocked()) {
      refreshTokenStore.remove(userId);

      // 특정 사용자의 access 토큰 redis에서 불러와서 블랙리스트에 추가
      String accessToken = accessTokenStore.get(userId);
      long expiration = jwtTokenProvider.getExpirationSecondsLeft(accessToken);
      accessTokenStore.blacklistAccessToken(accessToken, expiration);
      accessTokenStore.remove(userId);
    }

    return userId;
  }
}
