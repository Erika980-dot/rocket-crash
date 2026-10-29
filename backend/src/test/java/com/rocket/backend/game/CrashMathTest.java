package com.rocket.backend.game;

import com.rocket.backend.common.Crypto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CrashMathTest {

    private static final String SEED = "test-seed-for-simulation";

    @Test
    void sameSeedAndRoundAlwaysGiveSameCrash() {
        int a = CrashMath.crashPoint(SEED, 42);
        int b = CrashMath.crashPoint(SEED, 42);
        assertEquals(a, b);   // deterministic = verifiable
    }

    @Test
    void differentRoundsGiveDifferentCrashes() {
        assertNotEquals(CrashMath.crashPoint(SEED, 1), CrashMath.crashPoint(SEED, 2));
    }

    @Test
    void crashIsNeverBelowOneOrAboveCap() {
        for (long round = 1; round <= 100_000; round++) {
            int c = CrashMath.crashPoint(SEED, round);
            assertTrue(c >= 100 && c <= CrashMath.MAX_CRASH, "bad crash " + c);
        }
    }

    @Test
    void playerCanVerifyARound() {
        String seed = Crypto.randomHex32();
        String hash = Crypto.sha256Hex(seed);
        int crash = CrashMath.crashPoint(seed, 7);

        assertTrue(CrashMath.verify(seed, hash, 7, crash));          // honest round
        assertFalse(CrashMath.verify(seed, hash, 7, crash + 1));     // result was changed
        assertFalse(CrashMath.verify("other", hash, 7, crash));      // seed was swapped
    }

    @Test
    void multiplierStartsAt1xAndReaches2xAfterAbout11point6Seconds() {
        assertEquals(100, CrashMath.multiplierAt(0));
        long t = CrashMath.timeToReach(200);
        assertTrue(t > 11_500 && t < 11_600, "took " + t + " ms");
        assertTrue(Math.abs(CrashMath.multiplierAt(t) - 200) <= 1);
    }

    // ===== The simulation: what you'd show the game designer =====

    @Test
    void rtpIs97PercentForEveryCashOutTarget() {
        int rounds = 1_000_000;
        int[] targets = {150, 200, 500, 1000};   // 1.5x, 2x, 5x, 10x

        System.out.println("Target   Hit rate   RTP");
        for (int target : targets) {
            long hits = 0;
            for (long round = 1; round <= rounds; round++) {
                if (CrashMath.crashPoint(SEED, round) >= target) hits++;
            }
            double hitRate = hits / (double) rounds;
            double rtp = hitRate * target / 100.0;   // win rate × payout multiplier

            System.out.printf("%5.2fx   %6.2f%%   %6.2f%%%n", target / 100.0, hitRate * 100, rtp * 100);
            assertEquals(0.97, rtp, 0.02, "RTP off for target " + target);
        }
    }

    @Test
    void instantCrashAt1xHappensAbout4PercentOfTheTime() {
        int rounds = 1_000_000;
        long instant = 0;
        for (long round = 1; round <= rounds; round++) {
            if (CrashMath.crashPoint(SEED, round) == 100) instant++;
        }
        double rate = instant / (double) rounds;
        System.out.printf("Instant crash rate: %.2f%%%n", rate * 100);
        assertEquals(1 - 0.97 / 1.01, rate, 0.002);   // theory: about 3.96%
    }

    @Test
    void about1PercentOfRoundsReach100x() {
        int rounds = 100_000;
        long big = 0;
        for (long round = 1; round <= rounds; round++) {
            if (CrashMath.crashPoint(SEED, round) >= 10_000) big++;   // 10000 = 100.00x
        }
        double rate = big / (double) rounds;
        System.out.printf("Reached 100x: %.3f%%%n", rate * 100);
        assertTrue(rate > 0.008 && rate < 0.0115, "rate was " + rate);
    }
}