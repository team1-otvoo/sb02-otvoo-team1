package com.team1.otvoo.user.dto;

import com.team1.otvoo.user.entity.Role;
import java.util.UUID;

public record UserDtoCursorRequest (
    String cursor,
    UUID idAfter,
    int limit,
    SortBy sortBy,
    SortDirection sortDirection,
    String emailLike,
    Role roleEqual,
    Boolean locked

){
}
