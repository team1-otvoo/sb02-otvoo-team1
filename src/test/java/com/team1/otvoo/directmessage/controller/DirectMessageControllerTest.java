package com.team1.otvoo.directmessage.controller;

import com.team1.otvoo.directmessage.dto.DirectMessageDtoCursorResponse;
import com.team1.otvoo.directmessage.service.DirectMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DirectMessageControllerTest {

  @Mock
  private DirectMessageService directMessageService;

  @InjectMocks
  private DirectMessageController directMessageController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void getDirectMessages_ReturnsResponseEntityWithData() {
    // given
    UUID userId = UUID.randomUUID();
    String cursor = "2023-08-01T10:00:00Z";
    String idAfter = UUID.randomUUID().toString();
    int limit = 5;

    DirectMessageDtoCursorResponse expectedResponse = mock(DirectMessageDtoCursorResponse.class);
    when(directMessageService.getDirectMessageByuserId(userId, cursor, idAfter, limit))
        .thenReturn(expectedResponse);

    // when
    ResponseEntity<DirectMessageDtoCursorResponse> responseEntity =
        directMessageController.getDirectMessages(userId, cursor, idAfter, limit);

    // then
    assertNotNull(responseEntity);
    assertEquals(200, responseEntity.getStatusCodeValue());
    assertEquals(expectedResponse, responseEntity.getBody());

    verify(directMessageService).getDirectMessageByuserId(userId, cursor, idAfter, limit);
    verifyNoMoreInteractions(directMessageService);
  }
}