package com.team1.otvoo.clothes.event;

import com.team1.otvoo.clothes.entity.Clothes;
import java.util.UUID;

public record ClothesCreatedEvent(
    Clothes clothes,
    String imageUrl
) {
}
