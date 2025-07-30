package com.team1.otvoo.user.dto;

public record UserCreateRequest(
    String name,
    String email,
    String password
) {

}
