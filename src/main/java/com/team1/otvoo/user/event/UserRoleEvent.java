package com.team1.otvoo.user.event;

import com.team1.otvoo.user.entity.Role;
import com.team1.otvoo.user.entity.User;

public record UserRoleEvent(
    Role previousUserRole,
    User user
) {

}
