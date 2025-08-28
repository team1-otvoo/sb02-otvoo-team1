package com.team1.otvoo.feed.service;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.feed.entity.Feed;
import com.team1.otvoo.feed.entity.FeedLike;
import com.team1.otvoo.feed.event.FeedLikeEvent;
import com.team1.otvoo.feed.event.FeedUnlikeEvent;
import com.team1.otvoo.feed.repository.FeedLikeRepository;
import com.team1.otvoo.feed.repository.FeedRepository;
import com.team1.otvoo.security.CustomUserDetails;
import com.team1.otvoo.user.entity.User;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedLikeServiceImpl implements FeedLikeService {

  private final FeedLikeRepository feedLikeRepository;
  private final FeedRepository feedRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public void create(UUID feedId) {
    Feed feed = findFeed(feedId);
    User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication()
        .getPrincipal()).getUser();

    FeedLike feedLike = new FeedLike(feed, user);

    FeedLike savedFeedLike = feedLikeRepository.save(feedLike);
    feedRepository.incrementLikeCount(feedId);

    eventPublisher.publishEvent(new FeedLikeEvent(savedFeedLike));
  }

  @Override
  @Transactional
  public void delete(UUID feedId) {
    Feed feed = findFeed(feedId);
    User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication()
        .getPrincipal()).getUser();

    if(feed.getLikeCount() >= 1) {
      feedLikeRepository.deleteByFeed_IdAndLikedBy_Id(feed.getId(), user.getId());
      feedRepository.decrementLikerCount(feed.getId());
      eventPublisher.publishEvent(new FeedUnlikeEvent(feedId));
    }
  }

  private Feed findFeed(UUID id) {
    return feedRepository.findById(id).orElseThrow(
        () -> new RestException(ErrorCode.FEED_NOT_FOUND, Map.of("feedId", id)));
  }
}
