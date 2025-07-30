package com.team1.otvoo.user.dto;

import com.team1.otvoo.user.entity.Role;
import java.time.Instant;
import java.util.List;

public record UserDto (
    String id,
    Instant createdAt,
    String email,
    String name,
    Role role,
    List<String> linkedOAuthProviders,
    boolean locked
) {

}
