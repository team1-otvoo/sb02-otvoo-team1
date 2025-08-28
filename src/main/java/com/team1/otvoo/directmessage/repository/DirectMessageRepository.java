package com.team1.otvoo.directmessage.repository;

import com.team1.otvoo.directmessage.entity.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {
}
