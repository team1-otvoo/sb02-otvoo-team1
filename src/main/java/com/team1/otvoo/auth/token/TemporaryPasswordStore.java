package com.team1.otvoo.auth.token;

import java.time.Duration;

public interface TemporaryPasswordStore {
  void save(String email, TemporaryPassword tempPassword, Duration duration);
  TemporaryPassword get(String email);
  void remove(String email);
}