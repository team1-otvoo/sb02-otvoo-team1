package com.team1.otvoo.follow.dto;

import com.team1.otvoo.user.dto.UserSummary;
import java.time.Instant;
import java.util.UUID;

public record FollowCursorDto(
    UUID id,
    Instant createdAt,
    UserSummary followee,
    UserSummary follower
) {

}
