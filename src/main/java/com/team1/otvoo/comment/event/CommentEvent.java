package com.team1.otvoo.comment.event;

import com.team1.otvoo.comment.entity.FeedComment;

public record CommentEvent(
    FeedComment savedComment
) {

}
