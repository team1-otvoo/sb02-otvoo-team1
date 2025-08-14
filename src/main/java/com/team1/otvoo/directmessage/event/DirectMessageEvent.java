package com.team1.otvoo.directmessage.event;

import com.team1.otvoo.directmessage.entity.DirectMessage;

public record DirectMessageEvent(
    DirectMessage savedDirectMessage
) {

}
