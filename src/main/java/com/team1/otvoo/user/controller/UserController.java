package com.team1.otvoo.user.controller;

import com.team1.otvoo.user.dto.UserCreateRequest;
import com.team1.otvoo.user.dto.UserDto;
import com.team1.otvoo.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  @PostMapping
  ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreateRequest userCreateRequest) {

    UserDto userDto = userService.createUser(userCreateRequest);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(userDto);
  }


}
