package com.team1.otvoo.auth.token;

import java.util.UUID;

public interface AccessTokenStore {
  void save(UUID userId, String accessToken, long expirationSeconds);
  String get(UUID userId);
  void remove(UUID userId);
  void blacklistAccessToken(String accessToken, long expirationSeconds);
  boolean isBlacklisted(String accessToken);
}