package com.team1.otvoo.auth.token;

import lombok.Getter;

@Getter
public class TemporaryPassword {
  private final String password;
  private final long expirationTime;
  private final boolean expired;

  public TemporaryPassword() {
    this.password = "";
    this.expirationTime = 0;
    this.expired = false;
  }

  public TemporaryPassword(String password, long expirationTime) {
    this.password = password;
    this.expirationTime = expirationTime;
    this.expired = false;
  }

  public boolean isExpired() {
    return System.currentTimeMillis() > expirationTime;
  }
}