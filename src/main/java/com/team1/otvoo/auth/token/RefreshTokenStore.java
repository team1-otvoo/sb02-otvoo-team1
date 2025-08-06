package com.team1.otvoo.auth.token;

public interface RefreshTokenStore {
  void save(String userId, String refreshToken);
  String get(String userId);
  void remove(String userId);
  void blacklistAccessToken(String accessToken, long expirationSeconds);
  boolean isBlacklisted(String accessToken);
}