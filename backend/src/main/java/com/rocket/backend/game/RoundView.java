package com.rocket.backend.game;

import java.time.Instant;

public record RoundView(
        long id,
        RoundStatus status,
        String seedHash,
        int multiplier,
        Instant bettingEndsAt,
        Integer crashPoint,      // null until CRASHED  爆炸前是 null
        String serverSeed        // null until CRASHED  爆炸前是 null
) {
    static RoundView from(CurrentRound c) {
        boolean over = c.status() == RoundStatus.CRASHED;
        return new RoundView(
                c.id(), c.status(), c.seedHash(), c.multiplierNow(), c.bettingEndsAt(),
                over ? c.crashPoint() : null,     // reveal only after the crash  爆炸后才公开
                over ? c.serverSeed() : null);
    }
}