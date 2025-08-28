package com.team1.otvoo.directmessage.controller;

import com.team1.otvoo.directmessage.dto.DirectMessageDtoCursorResponse;
import com.team1.otvoo.directmessage.service.DirectMessageService;
import com.team1.otvoo.security.CustomUserDetails;
import com.team1.otvoo.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

  @Mock
  private CustomUserDetails customUserDetails;

  @Mock
  private User user;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  @DisplayName("사용자별 다이렉트 메시지 조회 성공")
  void getDirectMessages_ReturnsResponseEntityWithData() {
    // given
    UUID currentUserId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    String cursor = "2023-08-01T10:00:00Z";
    String idAfter = UUID.randomUUID().toString();
    int limit = 5;

    DirectMessageDtoCursorResponse expectedResponse = mock(DirectMessageDtoCursorResponse.class);

    when(customUserDetails.getUser()).thenReturn(user);
    when(user.getId()).thenReturn(currentUserId);

    when(directMessageService.getDirectMessagesBetweenUsers(currentUserId, otherUserId, cursor, idAfter, limit))
        .thenReturn(expectedResponse);

    // when
    ResponseEntity<DirectMessageDtoCursorResponse> responseEntity =
        directMessageController.getDirectMessages(customUserDetails, otherUserId, cursor, idAfter, limit);

    // then
    assertNotNull(responseEntity);
    assertEquals(200, responseEntity.getStatusCodeValue());
    assertEquals(expectedResponse, responseEntity.getBody());

    verify(customUserDetails).getUser();
    verify(user).getId();
    verify(directMessageService).getDirectMessagesBetweenUsers(currentUserId, otherUserId, cursor, idAfter, limit);
    verifyNoMoreInteractions(directMessageService, customUserDetails, user);
  }
}