package com.team1.otvoo.user.dto;

import com.team1.otvoo.user.entity.Gender;
import java.time.LocalDate;
import java.util.UUID;

public record ProfileDto (
    UUID userId,
    String name,
    Gender gender,
    LocalDate birthDate,
    Location location,
    Integer temperatureSensitivity,
    String profileImageUrl
) {
}
