package com.team1.otvoo.follow.dto;

import com.team1.otvoo.user.dto.UserSummary;
import java.util.UUID;

public record FollowDto(
    UUID id,
    UserSummary followee,
    UserSummary follower
) {

}
