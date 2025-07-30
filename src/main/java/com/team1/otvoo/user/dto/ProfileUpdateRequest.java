package com.team1.otvoo.user.dto;

import com.team1.otvoo.user.entity.Gender;
import java.time.LocalDate;

public record ProfileUpdateRequest(
    String name,
    Gender gender,
    LocalDate birthDate,
    Location location,
    Integer temperatureSensitivity
) {
}
