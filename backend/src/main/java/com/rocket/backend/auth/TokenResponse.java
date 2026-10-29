package com.rocket.backend.auth;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {}