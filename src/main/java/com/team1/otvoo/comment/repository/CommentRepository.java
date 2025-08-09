package com.team1.otvoo.comment.repository;

import com.team1.otvoo.comment.entity.FeedComment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<FeedComment, UUID> {

}
