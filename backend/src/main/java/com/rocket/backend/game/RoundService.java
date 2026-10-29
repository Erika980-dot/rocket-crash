package com.rocket.backend.game;

import com.rocket.backend.common.Crypto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoundService {

    private final RoundRepository roundRepository;

    /** Create a new round: secret seed → hash → crash point.  创建新回合 */
    @Transactional
    public Round openNewRound() {
        String seed = Crypto.randomHex32();

        Round r = new Round();
        r.setServerSeed(seed);
        r.setSeedHash(Crypto.sha256Hex(seed));
        r.setCrashPoint(0);                 // placeholder: the formula needs the round id first
        r.setStatus(RoundStatus.BETTING);
        roundRepository.save(r);            // INSERT → MySQL assigns the id  插入后才拿到 id

        r.setCrashPoint(CrashMath.crashPoint(seed, r.getId()));   // dirty checking → UPDATE at commit
        return r;
    }

    /** Betting closed, rocket launches.  停止下注,火箭起飞 */
    @Transactional
    public void launch(Long roundId, Instant launchedAt) {
        Round r = roundRepository.findById(roundId).orElseThrow();
        r.setStatus(RoundStatus.RUNNING);
        r.setStartedAt(toLocal(launchedAt));
    }

    /** Rocket exploded.  火箭爆炸 */
    @Transactional
    public void crash(Long roundId) {
        Round r = roundRepository.findById(roundId).orElseThrow();
        r.setStatus(RoundStatus.CRASHED);
        r.setCrashedAt(LocalDateTime.now());
        // Part 3C will add here: mark all PLACED bets in this round as LOST
        // 3C 会在这里加:把这一局还没提现的注单全部改成"输"
    }

    /** On startup: rounds left half-finished by a restart → CANCELLED.  启动时清理卡住的回合 */
    @Transactional
    public int cancelUnfinishedRounds() {
        List<Round> stuck = roundRepository.findByStatusIn(
                List.of(RoundStatus.BETTING, RoundStatus.RUNNING));
        stuck.forEach(r -> r.setStatus(RoundStatus.CANCELLED));
        return stuck.size();
    }

    private static LocalDateTime toLocal(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}