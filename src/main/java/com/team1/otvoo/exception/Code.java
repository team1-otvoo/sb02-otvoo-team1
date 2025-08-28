package com.team1.otvoo.exception;

import org.springframework.http.HttpStatus;

public interface Code {
  HttpStatus getStatus();
  String getMessage();
}