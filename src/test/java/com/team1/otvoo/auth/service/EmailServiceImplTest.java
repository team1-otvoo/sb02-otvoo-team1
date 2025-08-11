package com.team1.otvoo.auth.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

  @InjectMocks
  private EmailServiceImpl emailService;

  @Mock
  private JavaMailSender mailSender;

  private MimeMessage mimeMessage;

  @BeforeEach
  void setUp() {
    mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
  }

  @Test
  @DisplayName("임시 비밀번호 이메일 정상 전송")
  void sendTemporaryPassword_Success() {
    // given
    String toEmail = "user@test.com";
    String tempPassword = "temporary123";

    // when
    assertDoesNotThrow(() -> emailService.sendTemporaryPassword(toEmail, tempPassword));

    // then
    verify(mailSender).send(mimeMessage);
  }

  @Test
  @DisplayName("이메일 전송 실패 시 RuntimeException 발생")
  void sendTemporaryPassword_MessagingException_ShouldThrow() {
// given
    String toEmail = "user@test.com";
    String tempPassword = "temporary123";

    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

    doThrow(new RuntimeException("이메일 전송에 실패했습니다.")).when(mailSender).send(mimeMessage);

    // when
    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> emailService.sendTemporaryPassword(toEmail, tempPassword));

    // then
    assertEquals("이메일 전송에 실패했습니다.", exception.getMessage());
    verify(mailSender).send(mimeMessage);
  }
}