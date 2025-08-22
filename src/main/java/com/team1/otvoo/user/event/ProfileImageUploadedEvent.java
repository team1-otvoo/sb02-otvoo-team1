package com.team1.otvoo.user.event;

public record ProfileImageUploadedEvent(
    String objectKey,
    Integer width,
    Integer height
) {

}
