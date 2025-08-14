package com.team1.otvoo.feed.event;

import com.team1.otvoo.feed.entity.Feed;

public record FeedEvent(
    Feed savedFeed
) {

}
