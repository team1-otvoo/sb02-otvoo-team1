package com.team1.otvoo.notification.repository;

import com.team1.otvoo.notification.entity.AsyncTaskFailure;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsyncTaskFailureRepository extends JpaRepository<AsyncTaskFailure, UUID> {

}
