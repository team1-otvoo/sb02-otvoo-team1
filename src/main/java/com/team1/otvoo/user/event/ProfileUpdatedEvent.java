package com.team1.otvoo.user.event;

import java.util.UUID;

public record ProfileUpdatedEvent(
    UUID userId,
    String name
) {

}
