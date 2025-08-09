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

  @OneToMany(mappedBy = "clothes", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ClothesSelectedValue> selectedValues = new ArrayList<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public Clothes(User owner, String name, ClothesType type,
      List<ClothesSelectedValue> selectedValues) {
    this.owner = owner;
    this.name = name;
    this.type = type;
    for (ClothesSelectedValue value : selectedValues) {
      addSelectedValue(value);
    }
    this.createdAt = Instant.now();
  }

  public void addSelectedValue(ClothesSelectedValue value) {
    this.selectedValues.add(value);
    value.setClothes(this);
  }

  public void updateName(String newName) {
    if (newName != null && !newName.isBlank()) {
      this.name = newName;
    }
  }
  public void updateType(ClothesType newType) {
    if (newType != null) {
      this.type = newType;
    }
  }
  public void replaceSelectedValues(List<ClothesSelectedValue> newSelectedValues) {
    this.selectedValues.clear();
    for(ClothesSelectedValue value : newSelectedValues) {
      addSelectedValue(value);
    }
  }
}
