package com.rocket.backend.game;

import com.rocket.backend.common.Crypto;

/**
 * Pure math for Rocket Crash. No Spring, no database: easy to test.
 * All multipliers are in HUNDREDTHS: 100 = 1.00x, 250 = 2.50x.
 */
public final class CrashMath {

    public static final double RTP = 0.97;          // players get back 97% on average
    public static final double GROWTH_PER_MS = 0.00006;  // = 0.06 per second
    public static final int MIN_CRASH = 100;        // 1.00x
    public static final int MAX_CRASH = 1_000_000;  // 10,000.00x cap

    private CrashMath() {}

    /** The crash point for a round, decided by the secret seed. Same inputs → same result. */
    public static int crashPoint(String serverSeed, long roundId) {
        byte[] h = Crypto.hmacSha256(serverSeed, Long.toString(roundId));

        // Turn the first 52 bits of the hash into a number r between 0 and 1
        long v = 0;
        for (int i = 0; i < 7; i++) {
            v = (v << 8) | (h[i] & 0xFF);   // 7 bytes = 56 bits
        }
        v = v >>> 4;                         // keep 52 bits (a double holds exactly 52)
        double r = v / (double) (1L << 52);  // 0 <= r < 1

        long crash = (long) Math.floor(RTP * 100 / (1.0 - r));   // in hundredths
        return (int) Math.max(MIN_CRASH, Math.min(crash, MAX_CRASH));
    }

    /** The multiplier (hundredths) after the rocket has flown for elapsedMs. */
    public static int multiplierAt(long elapsedMs) {
        return (int) Math.floor(100 * Math.exp(GROWTH_PER_MS * elapsedMs));
    }

    /** How many ms after launch the rocket reaches a multiplier (hundredths). */
    public static long timeToReach(int multiplier) {
        return (long) Math.ceil(Math.log(multiplier / 100.0) / GROWTH_PER_MS);
    }

    /** Can a player verify a past round? Checks the seed matches its hash AND the result. */
    public static boolean verify(String serverSeed, String seedHash, long roundId, int crashPoint) {
        return Crypto.sha256Hex(serverSeed).equals(seedHash)
                && crashPoint(serverSeed, roundId) == crashPoint;
    }
}