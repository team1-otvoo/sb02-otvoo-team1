package com.team1.otvoo.comment.mapper;

import com.team1.otvoo.comment.dto.CommentDto;
import com.team1.otvoo.comment.entity.FeedComment;
import com.team1.otvoo.user.dto.AuthorDto;
import com.team1.otvoo.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface CommentMapper {
  @Mapping(source = "comment.id", target = "id")
  @Mapping(source = "comment.createdAt", target = "createdAt")
  @Mapping(source = "comment.feed.id", target = "feedId")
  @Mapping(source = "comment.content", target = "content")
  CommentDto toDto(FeedComment comment, AuthorDto author);
}
