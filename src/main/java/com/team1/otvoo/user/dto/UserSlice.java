package com.team1.otvoo.user.dto;

import com.team1.otvoo.user.entity.User;
import java.util.List;

public record UserSlice(
    List<User> content,
    boolean hasNext,
    long totalCount
) {

}
