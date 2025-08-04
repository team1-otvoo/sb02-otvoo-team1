package com.team1.otvoo.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

  private final JavaMailSender mailSender;

  @Override
  public void sendTemporaryPassword(String toEmail, String tempPassword) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

      helper.setTo(toEmail);
      helper.setSubject("[OTVOO] 임시 비밀번호 안내");
      helper.setText("""
          안녕하세요.

          요청하신 임시 비밀번호는 아래와 같습니다:

          임시 비밀번호: %s

          로그인 후 반드시 비밀번호를 변경해 주세요.

          감사합니다.
          """.formatted(tempPassword), false);

      mailSender.send(message);
      log.info("📧 임시 비밀번호 이메일 전송 완료: {}", toEmail);
    } catch (MessagingException e) {
      log.error("❌ 이메일 전송 실패: {}", e.getMessage());
      throw new RuntimeException("이메일 전송에 실패했습니다.");
    }
  }
}