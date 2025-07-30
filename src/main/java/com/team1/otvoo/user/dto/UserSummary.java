package com.team1.otvoo.user.dto;

import java.util.UUID;

public record UserSummary (
    UUID userId,
    String name,
    String profileImageUrl
){

}
