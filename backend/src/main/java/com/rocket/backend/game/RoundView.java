package com.rocket.backend.game;

public record RoundView(
        long id,
        RoundStatus status,
        String seedHash,
        int multiplier,
        long bettingEndsAtMs,     // when betting closes  下注截止时间
        Long launchedAtMs,        // null until RUNNING   起飞时间(飞行前是 null)
        long serverTimeMs,        // the server's clock right now  服务器当下的时间
        Integer crashPoint,       // null until CRASHED
        String serverSeed         // null until CRASHED
) {
    static RoundView from(CurrentRound c) {
        boolean over = c.status() == RoundStatus.CRASHED;
        return new RoundView(
                c.id(), c.status(), c.seedHash(), c.multiplierNow(),
                c.bettingEndsAt().toEpochMilli(),
                c.launchedAt() == null ? null : c.launchedAt().toEpochMilli(),
                System.currentTimeMillis(),
                over ? c.crashPoint() : null,
                over ? c.serverSeed() : null);
    }
}