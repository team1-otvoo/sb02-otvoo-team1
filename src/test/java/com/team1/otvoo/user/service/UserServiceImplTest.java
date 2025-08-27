package com.team1.otvoo.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.team1.otvoo.auth.token.AccessTokenStore;
import com.team1.otvoo.auth.token.RefreshTokenStore;
import com.team1.otvoo.auth.token.TemporaryPasswordStore;
import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.security.JwtTokenProvider;
import com.team1.otvoo.user.dto.ChangePasswordRequest;
import com.team1.otvoo.user.dto.Location;
import com.team1.otvoo.user.dto.ProfileDto;
import com.team1.otvoo.user.dto.ProfileUpdateRequest;
import com.team1.otvoo.user.dto.SortBy;
import com.team1.otvoo.user.dto.SortDirection;
import com.team1.otvoo.user.dto.UserCreateRequest;
import com.team1.otvoo.user.dto.UserDto;
import com.team1.otvoo.user.dto.UserDtoCursorRequest;
import com.team1.otvoo.user.dto.UserDtoCursorResponse;
import com.team1.otvoo.user.dto.UserLockUpdateRequest;
import com.team1.otvoo.user.dto.UserRoleUpdateRequest;
import com.team1.otvoo.user.dto.UserRow;
import com.team1.otvoo.user.dto.UserSlice;
import com.team1.otvoo.user.entity.Gender;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.Role;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.event.UserRoleEvent;
import com.team1.otvoo.user.mapper.ProfileMapper;
import com.team1.otvoo.user.mapper.TestUtils;
import com.team1.otvoo.user.mapper.UserMapper;
import com.team1.otvoo.user.repository.ProfileImageRepository;
import com.team1.otvoo.user.repository.ProfileRepository;
import com.team1.otvoo.user.repository.UserRepository;

import com.team1.otvoo.user.resolver.ProfileImageUrlResolver;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @InjectMocks
  private UserServiceImpl userService;

  @Mock
  private ProfileImageService profileImageService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ProfileRepository profileRepository;

  @Mock
  private ProfileImageRepository profileImageRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private UserMapper userMapper;

  @Mock
  private ProfileMapper profileMapper;

  @Mock
  private ProfileImageUrlResolver profileImageUrlResolver;

  @Mock
  private TemporaryPasswordStore temporaryPasswordStore;

  @Mock
  private RefreshTokenStore refreshTokenStore;

  @Mock
  private AccessTokenStore accessTokenStore;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  private UserCreateRequest request;

  @BeforeEach
  void setUp() {
    request = new UserCreateRequest(
        "testUser1",
        "testUser1@email.com",
        "password1234!"
    );
  }

  @DisplayName("커서 기반 사용자 조회 - 다음 페이지가 있는 경우")
  @Test
  void getUsers_hasNext_true() {
    // given
    UserDtoCursorRequest request = mock(UserDtoCursorRequest.class);
    given(request.sortBy()).willReturn(SortBy.EMAIL);
    given(request.sortDirection()).willReturn(SortDirection.ASCENDING);

    // rows: 마지막 row(row2)의 id/email만 필요
    UserRow row1 = mock(UserRow.class);
    UserRow row2 = mock(UserRow.class);
    UUID id2 = UUID.randomUUID();
    given(row2.id()).willReturn(id2);
    given(row2.email()).willReturn("bob@example.com");

    @SuppressWarnings("unchecked")
    UserSlice<UserRow> slice = mock(UserSlice.class);
    given(slice.content()).willReturn(List.of(row1, row2));
    given(slice.hasNext()).willReturn(true);
    given(slice.totalCount()).willReturn(42L);

    given(userRepository.searchUserRowWithCursor(request)).willReturn(slice);

    // mapper 결과 (row의 내부 필드 접근 안 하므로 추가 stubbing 불필요)
    Instant now = Instant.now();
    UserDto dto1 = new UserDto(UUID.randomUUID(), now.minusSeconds(1), "alice@example.com", "Alice", Role.USER, List.of(), false);
    UserDto dto2 = new UserDto(id2, now, "bob@example.com", "Bob", Role.USER, List.of(), false);
    given(userMapper.toUserDtoFromUserRow(row1, List.of())).willReturn(dto1);
    given(userMapper.toUserDtoFromUserRow(row2, List.of())).willReturn(dto2);

    // when
    UserDtoCursorResponse resp = userService.getUsers(request);

    // then
    assertThat(resp.data()).containsExactly(dto1, dto2);
    assertThat(resp.hasNext()).isTrue();
    assertThat(resp.totalCount()).isEqualTo(42L);
    assertThat(resp.nextCursor()).isEqualTo("bob@example.com"); // EMAIL 기준 → 마지막 row email
    assertThat(resp.nextIdAfter()).isEqualTo(id2);
    assertThat(resp.sortBy()).isEqualTo(SortBy.EMAIL);
    assertThat(resp.sortDirection()).isEqualTo(SortDirection.ASCENDING);
  }

  @DisplayName("커서 기반 사용자 조회 - 다음 페이지가 없는 경우")
  @Test
  void getUsers_hasNext_false() {
    // given
    UserDtoCursorRequest request = mock(UserDtoCursorRequest.class);
    given(request.sortBy()).willReturn(SortBy.CREATED_AT);
    given(request.sortDirection()).willReturn(SortDirection.DESCENDING);

    UserRow row = mock(UserRow.class);

    @SuppressWarnings("unchecked")
    UserSlice<UserRow> slice = mock(UserSlice.class);
    given(slice.content()).willReturn(List.of(row));
    given(slice.hasNext()).willReturn(false);
    given(slice.totalCount()).willReturn(1L);

    given(userRepository.searchUserRowWithCursor(request)).willReturn(slice);

    Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
    UserDto dto = new UserDto(UUID.randomUUID(), createdAt, "solo@example.com", "Solo", Role.USER, List.of(), false);
    given(userMapper.toUserDtoFromUserRow(row, List.of())).willReturn(dto);

    // when
    UserDtoCursorResponse resp = userService.getUsers(request);

    // then
    assertThat(resp.data()).containsExactly(dto);
    assertThat(resp.hasNext()).isFalse();
    assertThat(resp.totalCount()).isEqualTo(1L);
    assertThat(resp.nextCursor()).isNull();
    assertThat(resp.nextIdAfter()).isNull();
    assertThat(resp.sortBy()).isEqualTo(SortBy.CREATED_AT);
    assertThat(resp.sortDirection()).isEqualTo(SortDirection.DESCENDING);
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

    User savedUser = new User("testuser1@email.com", "encodedPassword1234!");
    TestUtils.setField(savedUser, "id", UUID.randomUUID());

    UserDto expectedDto = new UserDto(
        savedUser.getId(),
        Instant.now(),
        "testuser1@email.com",
        "testUser1",
        Role.USER,
        List.of(),
        false
    );
    given(userMapper.toUserDto(any(User.class), anyString())).willReturn(expectedDto);

    UserDto result = userService.createUser(request);

    assertThat(result).isEqualTo(expectedDto);
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
  @DisplayName("권한 변경 성공 시 리프레시/액세스 토큰 제거 및 블랙리스트 등록")
  void updateUserRole_success_logoutAndBlacklist() {
    // given
    UUID userId = UUID.randomUUID();

    User user = new User("user@example.com", "encoded");
    TestUtils.setField(user, "id", userId); // User id 설정 (mapper 호출 시 필요할 수 있음)

    // 초기 권한: USER
    Profile profile = new Profile("홍길동", user);

    given(profileRepository.findByUserIdWithUser(userId)).willReturn(Optional.of(profile));

    String accessToken = "access.token.value";
    long expiresIn = 3600L;
    given(accessTokenStore.get(userId)).willReturn(accessToken);
    given(jwtTokenProvider.getExpirationSecondsLeft(accessToken)).willReturn(expiresIn);

    // mapper 결과
    UserDto expected = new UserDto(
        userId,
        Instant.now(),
        "user@example.com",
        "홍길동",
        Role.ADMIN,
        List.of(),
        false
    );
    given(userMapper.toUserDto(user, "홍길동")).willReturn(expected);

    // when
    UserDto result = userService.updateUserRole(userId, new UserRoleUpdateRequest(Role.ADMIN));

    // then
    assertThat(result).isEqualTo(expected);
    // 로그아웃 처리(토큰 제거 및 블랙리스트 등록) 검증
    verify(refreshTokenStore).remove(userId);
    verify(accessTokenStore).get(userId);
    verify(jwtTokenProvider).getExpirationSecondsLeft(accessToken);
    verify(accessTokenStore).blacklistAccessToken(accessToken, expiresIn);
    verify(accessTokenStore).remove(userId);

    ArgumentCaptor<UserRoleEvent> eventCaptor = ArgumentCaptor.forClass(UserRoleEvent.class);
    then(eventPublisher).should().publishEvent(eventCaptor.capture());

    UserRoleEvent publishedEvent = eventCaptor.getValue();
    assertThat(publishedEvent.user()).isEqualTo(user);

  }

  @Test
  @DisplayName("비밀번호 변경 성공")
  void changePassword_success() {
    UUID userId = UUID.randomUUID();
    String rawPassword = "newPassword123!";
    String encodedPassword = "encodedPassword123!";
    ChangePasswordRequest request = new ChangePasswordRequest(rawPassword);
    User user = mock(User.class);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(passwordEncoder.encode(rawPassword)).willReturn(encodedPassword);

    userService.changePassword(userId, request);

    then(user).should().changePassword(encodedPassword);
  }

  @Test
  @DisplayName("존재하지 않는 사용자 ID일 경우 예외 발생")
  void changePassword_userNotFound() {
    UUID userId = UUID.randomUUID();
    ChangePasswordRequest request = new ChangePasswordRequest("newPassword123!");

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> userService.changePassword(userId, request))
        .isInstanceOf(RestException.class)
        .hasMessage(ErrorCode.NOT_FOUND.getMessage());

    then(userRepository).shouldHaveNoMoreInteractions();
  }

  @DisplayName("프로필 업데이트 성공 - 이미지 포함 (서비스 변경 반영)")
  @Test
  void updateProfile_success_withImage() {
    // given
    UUID userId = UUID.randomUUID();
    UUID profileId = UUID.randomUUID();

    User user = new User("user@example.com", "encodedPassword");
    TestUtils.setField(user, "id", userId);

    Profile profile = new Profile("홍길동", user);
    TestUtils.setField(profile, "id", profileId);

    MultipartFile newImageFile = mock(MultipartFile.class);

    // 서비스는 이제 repository 직접 접근 없이 service 메서드로 URL만 받음
    String resolvedUrl = "https://cdn.example.com/newImage.png";
    ProfileDto expectedDto = mock(ProfileDto.class);

    given(profileRepository.findByUserId(userId)).willReturn(Optional.of(profile));
    given(profileImageService.replaceProfileImageAndGetUrl(profile, newImageFile)).willReturn(resolvedUrl);
    given(profileMapper.toProfileDto(userId, profile, resolvedUrl)).willReturn(expectedDto);

    // when
    ProfileDto result = userService.updateProfile(
        userId,
        new ProfileUpdateRequest("홍길동", Gender.MALE, LocalDate.now(), null, 1),
        newImageFile
    );

    // then
    assertThat(result).isEqualTo(expectedDto);
    verify(profileImageService).replaceProfileImageAndGetUrl(profile, newImageFile);
    verify(profileMapper).toProfileDto(userId, profile, resolvedUrl);

    // 더 이상 ProfileImageRepository와의 상호작용이 없어야 함
    verifyNoInteractions(profileImageRepository);
  }

  @Test
  @DisplayName("기존 비밀번호와 동일하면 예외 발생")
  void changePassword_samePassword() {
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

    assertThatThrownBy(() -> userService.changePassword(userId, request))
        .isInstanceOf(RestException.class)
        .hasMessage(ErrorCode.SAME_AS_OLD_PASSWORD.getMessage());

    then(user).should().changePassword(encodedPassword);
  }

  @Test
  @DisplayName("프로필 조회 성공")
  void getUserProfile_success() {
    UUID userId = UUID.randomUUID();
    UUID profileId = UUID.randomUUID();

    User user = new User("test@email.com", "encoded");
    TestUtils.setField(user, "id", userId);

    String username = "홍길동";
    Profile profile = new Profile(username, user);
    TestUtils.setField(profile, "id", profileId);

    // 서비스는 resolver를 사용하므로 이 부분을 스텁해야 합니다.
    String profileImageUrl = "imageUrl";

    // given
    given(profileRepository.findByUserId(userId)).willReturn(Optional.of(profile));
    given(profileImageUrlResolver.resolve(profileId)).willReturn(profileImageUrl);

    // mapper 결과를 기대값으로 고정
    Location location = new Location(37.5665, 126.9780, 3, 4, List.of("서울특별시", "강남구", "역삼동"));
    ProfileDto expectedDto = new ProfileDto(
        userId,
        username,
        Gender.MALE,
        LocalDate.of(1990, 1, 1),
        location,
        1,
        profileImageUrl
    );
    given(profileMapper.toProfileDto(userId, profile, profileImageUrl)).willReturn(expectedDto);

    // when
    ProfileDto result = userService.getUserProfile(userId);

    // then
    assertThat(result).isEqualTo(expectedDto);
  }


  @Test
  @DisplayName("계정 잠금 상태 변경 성공 - 잠금 처리 시 토큰 삭제 및 블랙리스트 등록")
  void changeLock_success_locked() {
    // given
    UUID userId = UUID.randomUUID();
    User user = mock(User.class); // ✅ mock 처리

    given(userRepository.findById(userId)).willReturn(Optional.of(user));

    // isLocked() == true 인 상태
    given(user.isLocked()).willReturn(true);

    String accessToken = "access.token.value";
    long expiration = 3600L;

    given(accessTokenStore.get(userId)).willReturn(accessToken);
    given(jwtTokenProvider.getExpirationSecondsLeft(accessToken)).willReturn(expiration);

    // when
    UUID result = userService.changeLock(userId, new UserLockUpdateRequest(true));

    // then
    assertThat(result).isEqualTo(userId);
    verify(refreshTokenStore).remove(userId);
    verify(accessTokenStore).get(userId);
    verify(jwtTokenProvider).getExpirationSecondsLeft(accessToken);
    verify(accessTokenStore).blacklistAccessToken(accessToken, expiration);
    verify(accessTokenStore).remove(userId);
  }

}