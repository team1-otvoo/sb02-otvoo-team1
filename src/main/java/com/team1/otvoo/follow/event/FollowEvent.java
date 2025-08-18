package com.team1.otvoo.follow.event;

import com.team1.otvoo.user.entity.User;

public record FollowEvent(
    User follower,
    User followee
) {

}
