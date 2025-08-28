package com.team1.otvoo.security;

import com.team1.otvoo.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class AdminInitializer implements ApplicationRunner {

  private final AuthService authService;

  @Override
  public void run(ApplicationArguments args) {
    authService.initAdmin();
  }
}