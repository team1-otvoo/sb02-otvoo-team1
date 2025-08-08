package com.team1.otvoo.auth.dto;

public record SignInRequest(
    String email,
    String password
) {

}