package com.team1.otvoo.clothes.event;

import java.util.UUID;

public record ClothesUpdatedEvent(
    UUID clothesId
) {

}
