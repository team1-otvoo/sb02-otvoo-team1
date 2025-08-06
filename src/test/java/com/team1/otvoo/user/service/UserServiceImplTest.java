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

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
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
import com.team1.otvoo.user.dto.UserSlice;
import com.team1.otvoo.user.entity.Gender;
import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.ProfileImage;
import com.team1.otvoo.user.entity.Role;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.mapper.ProfileMapper;
import com.team1.otvoo.user.mapper.TestUtils;
import com.team1.otvoo.user.mapper.UserMapper;
import com.team1.otvoo.user.projection.UserNameView;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

  private UserCreateRequest request;

  @BeforeEach
  void setUp() {
    request = new UserCreateRequest(
        "testUser1",
        "testUser1@email.com",
        "password1234!"
    );
  }

  @Test
  @DisplayName("사용자 목록 조회 성공 - 커서 포함 응답 생성")
  void getUsers_success() {
    // given
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    Instant createdAt1 = Instant.parse("2024-01-01T10:00:00Z");
    Instant createdAt2 = Instant.parse("2024-01-01T11:00:00Z");

    User user1 = new User("alice@example.com", "pw1");
    User user2 = new User("bob@example.com", "pw2");

    TestUtils.setField(user1, "id", id1);
    TestUtils.setField(user2, "id", id2);
    TestUtils.setField(user1, "createdAt", createdAt1);
    TestUtils.setField(user2, "createdAt", createdAt2);

    List<User> userList = List.of(user1, user2);
    long totalCount = 100L;
    boolean hasNext = true;

    UserDtoCursorRequest request = new UserDtoCursorRequest(
        null,
        null,
        10,
        SortBy.EMAIL,
        SortDirection.ASCENDING,
        null,
        null,
        null
    );

    // 프로젝션 결과 Mock
    UserNameView view1 = mock(UserNameView.class);
    UserNameView view2 = mock(UserNameView.class);
    given(view1.getUserId()).willReturn(id1);
    given(view1.getName()).willReturn("Alice");
    given(view2.getUserId()).willReturn(id2);
    given(view2.getName()).willReturn("Bob");

    given(userRepository.searchUsersWithCursor(request)).willReturn(new UserSlice(userList, hasNext, totalCount));
    given(profileRepository.findUserNamesByUserIds(List.of(id1, id2))).willReturn(List.of(view1, view2));

    UserDto dto1 = new UserDto(id1, createdAt1, "alice@example.com", "Alice", Role.USER, List.of(), false);
    UserDto dto2 = new UserDto(id2, createdAt2, "bob@example.com", "Bob", Role.USER, List.of(), false);

    given(userMapper.toUserDto(user1, "Alice")).willReturn(dto1);
    given(userMapper.toUserDto(user2, "Bob")).willReturn(dto2);

    // when
    UserDtoCursorResponse response = userService.getUsers(request);

    // then
    assertThat(response.data()).containsExactly(dto1, dto2);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.totalCount()).isEqualTo(totalCount);
    assertThat(response.nextIdAfter()).isEqualTo(id2);
    assertThat(response.nextCursor()).isEqualTo(user2.getEmail());
    assertThat(response.sortBy()).isEqualTo(SortBy.EMAIL);
    assertThat(response.sortDirection()).isEqualTo(SortDirection.ASCENDING);
  }

  @Test
  @DisplayName("사용자 목록이 비어 있을 경우 - 커서 없음")
  void getUsers_emptyList() {
    UserDtoCursorRequest request = new UserDtoCursorRequest(
        null,
        null,
        10,
        SortBy.CREATED_AT,
        SortDirection.DESCENDING,
        null,
        null,
        null
    );

    given(userRepository.searchUsersWithCursor(request))
        .willReturn(new UserSlice(List.of(), false, 0L));

    UserDtoCursorResponse response = userService.getUsers(request);

    assertThat(response.data()).isEmpty();
    assertThat(response.hasNext()).isFalse();
    assertThat(response.totalCount()).isEqualTo(0);
    assertThat(response.nextIdAfter()).isNull();
    assertThat(response.nextCursor()).isNull();
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

  @DisplayName("프로필 업데이트 성공 - 이미지 포함")
  @Test
  void updateProfile_success_withImage() {
    // given
    UUID userId = UUID.randomUUID();
    UUID profileId = UUID.randomUUID();

    User user = new User("user@example.com", "encodedPassword");
    TestUtils.setField(user, "id", userId);

    Profile profile = new Profile("홍길동", user);
    TestUtils.setField(profile, "id", profileId);

    // 기존 이미지
    ProfileImage oldImage = mock(ProfileImage.class);
    given(profileRepository.findByUserId(userId)).willReturn(Optional.of(profile));
    given(profileImageRepository.findByProfileId(profileId)).willReturn(Optional.of(oldImage));

    // 새 이미지 저장
    MultipartFile newImageFile = mock(MultipartFile.class);
    ProfileImage newImage = mock(ProfileImage.class);

    ProfileDto expectedDto = mock(ProfileDto.class);

    given(profileImageService.createProfileImage(newImageFile, profile)).willReturn(newImage);
    given(profileImageUrlResolver.resolve(profileId)).willReturn("https://cdn.example.com/newImage.png");
    given(profileMapper.toProfileDto(userId, profile, "https://cdn.example.com/newImage.png")).willReturn(expectedDto);

    // when
    ProfileDto result = userService.updateProfile(userId, new ProfileUpdateRequest("홍길동", Gender.MALE, LocalDate.now(), null, 1), newImageFile);

    // then
    verify(profileImageService).deleteProfileImage(oldImage);
    verify(profileImageRepository).delete(oldImage);
    verify(profileImageRepository).save(newImage);
    verify(profileImageService).createProfileImage(newImageFile, profile);
    verify(profileMapper).toProfileDto(userId, profile, "https://cdn.example.com/newImage.png");

    assertThat(result).isEqualTo(expectedDto);
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

    String profileImageUrl = "imageUrl";
    ProfileImage profileImage = new ProfileImage(
        profileImageUrl,
        "original.png",
        "image/png",
        100L,
        100,
        100,
        profile
    );

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

    ProfileDto expectedDto = new ProfileDto(
        userId,
        username,
        Gender.MALE,
        LocalDate.of(1990, 1, 1),
        location,
        1,
        profileImageUrl
    );

    given(profileRepository.findByUserId(userId)).willReturn(Optional.of(profile));
    given(profileImageRepository.findByProfileId(profileId)).willReturn(Optional.of(profileImage));
    given(profileMapper.toProfileDto(userId, profile, profileImage.getImageUrl()))
        .willReturn(expectedDto);

    ProfileDto result = userService.getUserProfile(userId);

    assertThat(result).isEqualTo(expectedDto);
  }
}