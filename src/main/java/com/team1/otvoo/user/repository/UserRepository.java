package com.team1.otvoo.user.repository;

import com.team1.otvoo.user.entity.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, UserQueryDslRepository{

  boolean existsByEmail(String email);
}
