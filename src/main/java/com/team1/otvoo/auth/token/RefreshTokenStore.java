package com.team1.otvoo.auth.token;

import java.util.UUID;

public interface RefreshTokenStore {
  void save(UUID userId, String refreshToken);
  String get(UUID userId);
  void remove(UUID userId);
}