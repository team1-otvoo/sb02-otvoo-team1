package com.team1.otvoo.feed.event;

import com.team1.otvoo.feed.entity.FeedLike;

public record FeedLikeEvent(
    FeedLike savedFeedLike
) {

}
