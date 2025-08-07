package com.team1.otvoo.auth.token;

public interface AccessTokenStore {
  void save(String userId, String accessToken, long expirationSeconds);
  String get(String userId);
  void remove(String userId);
  void blacklistAccessToken(String accessToken, long expirationSeconds);
  boolean isBlacklisted(String accessToken);
}