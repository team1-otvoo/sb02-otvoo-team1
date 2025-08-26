package com.team1.otvoo.feed.event;

import com.team1.otvoo.feed.dto.FeedDto;

public record FeedCreatedEvent(
    FeedDto feedDto
) {

}
