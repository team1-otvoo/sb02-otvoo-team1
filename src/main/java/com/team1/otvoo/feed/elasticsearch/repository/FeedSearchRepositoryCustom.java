package com.team1.otvoo.feed.elasticsearch.repository;

import com.team1.otvoo.feed.dto.FeedDto;
import com.team1.otvoo.feed.dto.FeedSearchCondition;
import org.springframework.data.domain.Slice;

public interface FeedSearchRepositoryCustom {
  Slice<FeedDto> searchFeeds(FeedSearchCondition condition);
}
