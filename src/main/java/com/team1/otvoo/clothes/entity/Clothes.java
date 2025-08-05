package com.team1.otvoo.clothes.entity;

import com.team1.otvoo.user.entity.User;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clothes")
@Getter
@NoArgsConstructor
public class Clothes {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @Column(nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ClothesType type;

  @Column
  private String imageUrl;

  @OneToMany(mappedBy = "clothes", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ClothesSelectedValue> selectedValues = new ArrayList<>();

  @Column(name = "created_at", nullable = false,updatable = false)
  private Instant createdAt;

}
