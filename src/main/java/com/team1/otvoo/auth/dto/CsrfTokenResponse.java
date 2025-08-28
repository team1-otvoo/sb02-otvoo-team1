package com.team1.otvoo.auth.dto;

public record CsrfTokenResponse(
    String headerName,
    String token,
    String parameterName
) {

}