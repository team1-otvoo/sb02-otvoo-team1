package com.team1.otvoo.user.dto;

import com.team1.otvoo.user.entity.Role;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserDto (
    UUID id,
    Instant createdAt,
    String email,
    String name,
    Role role,
    List<String> linkedOAuthProviders,
    boolean locked
) {

}
