package com.team1.otvoo.user.dto;

import java.util.List;

public record Location(
    double latitude,
    double longitude,
    int x,
    int y,
    List<String> locationNames
) {
}
