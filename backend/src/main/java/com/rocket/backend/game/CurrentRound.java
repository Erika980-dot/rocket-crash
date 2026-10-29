package com.rocket.backend.game;

import java.time.Duration;
import java.time.Instant;

/** An immutable snapshot of the round being played right now.  当前回合的快照(不可修改) */
public record CurrentRound(
        long id,
        RoundStatus status,
        String seedHash,
        int crashPoint,       // internal only, never sent to players before CRASHED
        String serverSeed,    // internal only
        Instant bettingEndsAt,
        Instant launchedAt
) {
    static CurrentRound betting(Round r, Instant bettingEndsAt) {
        return new CurrentRound(r.getId(), RoundStatus.BETTING, r.getSeedHash(),
                r.getCrashPoint(), r.getServerSeed(), bettingEndsAt, null);
    }

    CurrentRound running(Instant launchedAt) {
        return new CurrentRound(id, RoundStatus.RUNNING, seedHash, crashPoint, serverSeed,
                bettingEndsAt, launchedAt);
    }

    CurrentRound crashed() {
        return new CurrentRound(id, RoundStatus.CRASHED, seedHash, crashPoint, serverSeed,
                bettingEndsAt, launchedAt);
    }

    /** The multiplier right now, from the SERVER clock.  用服务器时间算出现在的倍数 */
    public int multiplierNow() {
        return switch (status) {
            case RUNNING -> Math.min(
                    CrashMath.multiplierAt(Duration.between(launchedAt, Instant.now()).toMillis()),
                    crashPoint);
            case CRASHED -> crashPoint;
            default -> 100;   // BETTING / CANCELLED: still 1.00x
        };
    }
}