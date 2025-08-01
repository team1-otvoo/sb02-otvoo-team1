package com.team1.otvoo.user.entity;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User{
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 100)
  private String email;

  @Column(nullable = false, length = 100)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private Role role;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "profile_id", nullable = false, unique = true)
  private Profile profile;

  @Column(name = "follower_count")
  private Long followerCount = 0L;

  @Column(name = "following_count")
  private Long followingCount = 0L;

  @Column
  private boolean locked = false;

  @Builder
  public User(String email, String password, Profile profile) {
    this.email = email;
    this.password = password;
    this.profile = profile;

    this.role = Role.USER;
    this.createdAt = Instant.now();
    this.followerCount = 0L;
    this.followingCount = 0L;
    this.locked = false;
  }

  public void changePassword(String password) {
    if (password.equals(this.password)) {
      throw new RestException(ErrorCode.SAME_AS_OLD_PASSWORD);
    }

    this.password = password;
  }

  public void increaseFollowerCount(){
    followerCount++;
  }

  public void decreaseFollowerCount(){
    if (this.followerCount > 0) {
      this.followerCount--;
    }
  }

  public void increaseFollowingCount(){
    followingCount++;
  }

  public void decreaseFollowingCount(){
    if (this.followingCount > 0) {
      this.followingCount--;
    }
  }

}

