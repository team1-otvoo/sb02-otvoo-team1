package com.team1.otvoo.feed.repository;

import com.team1.otvoo.feed.dto.FeedSearchCondition;
import com.team1.otvoo.feed.entity.Feed;
import org.springframework.data.domain.Slice;

public interface FeedRepositoryCustom {
  Slice<Feed> searchByCondition(FeedSearchCondition condition);

}
