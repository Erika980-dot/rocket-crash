package com.rocket.backend.common;

import org.springframework.http.HttpStatus;

public class GameException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public GameException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }
}
