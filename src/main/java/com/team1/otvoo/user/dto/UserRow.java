package com.team1.otvoo.user.dto;

import com.team1.otvoo.user.entity.Role;
import java.time.Instant;
import java.util.UUID;

public record UserRow (
    UUID id,
    Instant createdAt,
    String email,
    String name,
    Role role,
    boolean locked
) {

}
