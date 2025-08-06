package com.team1.otvoo.user.controller;

import com.team1.otvoo.user.dto.ChangePasswordRequest;
import com.team1.otvoo.user.dto.ProfileDto;
import com.team1.otvoo.user.dto.ProfileUpdateRequest;
import com.team1.otvoo.user.dto.SortBy;
import com.team1.otvoo.user.dto.SortDirection;
import com.team1.otvoo.user.dto.UserCreateRequest;
import com.team1.otvoo.user.dto.UserDto;
import com.team1.otvoo.user.dto.UserDtoCursorRequest;
import com.team1.otvoo.user.dto.UserDtoCursorResponse;
import com.team1.otvoo.user.entity.Role;
import com.team1.otvoo.user.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

  private final UserService userService;

  @GetMapping
  ResponseEntity<UserDtoCursorResponse> getUserList(
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam int limit,
      @RequestParam SortBy sortBy,
      @RequestParam SortDirection sortDirection,
      @RequestParam(required = false) String emailLike,
      @RequestParam(required = false) Role roleEqual,
      @RequestParam(required = false) Boolean locked
  ) {
    log.info("GET /api/users - 계정 목록 조회 요청");

    UserDtoCursorRequest request = new UserDtoCursorRequest(
        cursor,
        idAfter,
        limit,
        sortBy,
        sortDirection,
        emailLike,
        roleEqual,
        locked
    );

    UserDtoCursorResponse response = userService.getUsers(request);

    log.info("GET /api/users - 계정 목록 조회 완료");

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(response);
  }

  @PostMapping
  ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreateRequest userCreateRequest) {
    log.info("POST /api/users - 회원 가입 요청: email={}, username={}",
        userCreateRequest.email(), userCreateRequest.name());

    UserDto userDto = userService.createUser(userCreateRequest);

    log.info("회원 가입 완료: userId={}", userDto.id());
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(userDto);
  }

  @GetMapping("/{userId}/profiles")
  ResponseEntity<ProfileDto> getUserProfile(@PathVariable UUID userId) {
    log.info("GET /api/users/{userId}/profiles} - 프로필 조회 요청: userId={}", userId);

    ProfileDto dto = userService.getUserProfile(userId);

    log.info("프로필 조회 완료: userId={}", dto.userId());

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(dto);
  }

  @PatchMapping(
      value = "/{userId}/profiles",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  ResponseEntity<ProfileDto> updateUserProfile(
      @PathVariable UUID userId,
      @RequestPart("request") ProfileUpdateRequest request,
      @RequestPart(value = "image", required = false) MultipartFile imageFile
  ) {
    log.info("PATCH /api/users/{userId}/profiles} - 프로필 업데이트 요청: userId={}", userId);

    ProfileDto dto = userService.updateProfile(userId, request, imageFile);

    log.info("프로필 업데이트 완료: userId={}", dto.userId());

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(dto);
  }

  @PatchMapping("/{userId}/password")
  ResponseEntity<Void> changePassword(
      @PathVariable UUID userId,
      @Valid @RequestBody ChangePasswordRequest changePasswordRequest
  ) {
    log.info("PATCH /api/users/{}/password - 비밀번호 변경 요청", userId);
    userService.changePassword(userId, changePasswordRequest);

    log.info("비밀번호 변경 완료: userId={}", userId);
    return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
  }
}
