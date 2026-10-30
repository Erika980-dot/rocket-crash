package com.rocket.backend.game;

public record RecentRound(long id, int crashPoint, String seedHash, String serverSeed) {
    static RecentRound from(Round r) {
        return new RecentRound(r.getId(), r.getCrashPoint(), r.getSeedHash(), r.getServerSeed());
    }
}