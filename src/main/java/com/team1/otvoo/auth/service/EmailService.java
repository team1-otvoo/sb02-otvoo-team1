package com.team1.otvoo.auth.service;

public interface EmailService {
  void sendTemporaryPassword(String toEmail, String tempPassword);
}