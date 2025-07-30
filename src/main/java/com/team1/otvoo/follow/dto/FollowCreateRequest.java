package com.team1.otvoo.follow.dto;

import java.util.UUID;

public record FollowCreateRequest(
    UUID followeeId,
    UUID followerId
) {

}
