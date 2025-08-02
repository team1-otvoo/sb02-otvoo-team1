package com.team1.otvoo.user.controller;

import com.team1.otvoo.user.dto.ChangePasswordRequest;
import com.team1.otvoo.user.dto.UserCreateRequest;
import com.team1.otvoo.user.dto.UserDto;
import com.team1.otvoo.user.dto.UserDtoCursorRequest;
import com.team1.otvoo.user.dto.UserDtoCursorResponse;
import com.team1.otvoo.user.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

  private final UserService userService;

  @GetMapping
  ResponseEntity<UserDtoCursorResponse> getUserList(
      @RequestBody UserDtoCursorRequest request
  ) {
    log.info("GET /api/users - 계정 목록 조회 요청");

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
