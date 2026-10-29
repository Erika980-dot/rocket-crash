package com.rocket.backend.game;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j                      // Lombok: gives us a "log" object for logging
@Component
@RequiredArgsConstructor
public class RoundEngine {

    static final long BETTING_MS = 5_000;   // 下注时间 5 秒
    static final long PAUSE_MS   = 3_000;   // 爆炸后休息 3 秒

    private final RoundService roundService;
    private final AtomicReference<CurrentRound> current = new AtomicReference<>();
    private volatile boolean running = true;
    private Thread thread;

    /** Runs once, when Spring has finished starting.  Spring 启动完成后执行一次 */
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        int cancelled = roundService.cancelUnfinishedRounds();
        if (cancelled > 0) {
            log.warn("Cancelled {} unfinished round(s) from the last run", cancelled);
        }
        thread = Thread.ofVirtual().name("round-engine").start(this::runLoop);
    }

    /** Runs when the app shuts down (red ■).  关闭程序时执行 */
    @PreDestroy
    public void stop() {
        running = false;
        if (thread != null) thread.interrupt();   // wake it from sleep so it can exit
    }

    /** What the rest of the app reads.  给其他代码读取当前回合 */
    public CurrentRound current() {
        return current.get();
    }

    private void runLoop() {
        while (running) {
            try {
                playOneRound();
                Thread.sleep(PAUSE_MS);
            } catch (InterruptedException e) {
                return;                                  // shutting down  程序关闭,退出循环
            } catch (Exception e) {
                log.error("Round failed, retrying in 3s", e);   // DB hiccup etc. — don't die
                try { Thread.sleep(3_000); } catch (InterruptedException ie) { return; }
            }
        }
    }

    private void playOneRound() throws InterruptedException {
        // 1. BETTING
        Round r = roundService.openNewRound();
        current.set(CurrentRound.betting(r, Instant.now().plusMillis(BETTING_MS)));
        log.info("Round {} BETTING  hash={}", r.getId(), r.getSeedHash());   // the HASH, never the seed!
        Thread.sleep(BETTING_MS);

        // 2. RUNNING
        Instant launchedAt = Instant.now();
        roundService.launch(r.getId(), launchedAt);
        current.set(current.get().running(launchedAt));
        log.info("Round {} RUNNING", r.getId());

        long flightMs = CrashMath.timeToReach(r.getCrashPoint());   // e.g. 2.50x → about 15,272 ms
        Thread.sleep(flightMs);

        // 3. CRASHED
        roundService.crash(r.getId());
        current.set(current.get().crashed());
        log.info("Round {} CRASHED at {}x", r.getId(), r.getCrashPoint() / 100.0);
    }
}