package com.team1.otvoo.auth.dto;

public record SignInResponse(
    String accessToken,
    String refreshToken,
    boolean usingTemporaryPassword
) {
}
