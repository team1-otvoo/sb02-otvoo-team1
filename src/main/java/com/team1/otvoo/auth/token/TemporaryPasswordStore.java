package com.team1.otvoo.auth.token;

import java.time.Duration;
import java.util.UUID;

public interface TemporaryPasswordStore {
  void save(UUID userId, TemporaryPassword tempPassword, Duration duration);
  TemporaryPassword get(UUID userId);
  void remove(UUID userId);
}