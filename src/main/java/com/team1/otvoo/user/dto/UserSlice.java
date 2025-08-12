package com.team1.otvoo.user.dto;

import java.util.List;

public record UserSlice<T>(
    List<T> content,
    boolean hasNext,
    long totalCount
) {

}
