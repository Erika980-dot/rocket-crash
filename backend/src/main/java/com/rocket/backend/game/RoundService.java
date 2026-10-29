package com.rocket.backend.game;

import com.rocket.backend.bet.Bet;
import com.rocket.backend.bet.BetRepository;
import com.rocket.backend.bet.BetStatus;
import com.rocket.backend.common.Crypto;
import com.rocket.backend.ledger.LedgerEntry;
import com.rocket.backend.ledger.LedgerRepository;
import com.rocket.backend.wallet.WalletRepository;
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
    private final BetRepository betRepository;
    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;

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
    public int crash(Long roundId) {
        Round r = roundRepository.findById(roundId).orElseThrow();
        r.setStatus(RoundStatus.CRASHED);
        r.setCrashedAt(LocalDateTime.now());

        // Everyone still flying loses. Their money was already taken at bet time.
        // 还在飞的人全部判输。钱在下注时已经扣了,这里只改状态。
        return betRepository.settleLosers(roundId, BetStatus.PLACED, BetStatus.LOST);
    }

    /** On startup: rounds left half-finished by a restart → CANCELLED.  启动时清理卡住的回合 */
    @Transactional
    public int cancelUnfinishedRounds() {
        List<Round> stuck = roundRepository.findByStatusIn(
                List.of(RoundStatus.BETTING, RoundStatus.RUNNING));

        for (Round r : stuck) {
            r.setStatus(RoundStatus.CANCELLED);

            // Give every unsettled bet its money back.  把还没结算的注单全部退款
            for (Bet b : betRepository.findByRoundIdAndStatus(r.getId(), BetStatus.PLACED)) {
                b.setStatus(BetStatus.REFUNDED);
                walletRepository.credit(b.getUserId(), b.getAmount());
                ledgerRepository.save(LedgerEntry.of(b.getUserId(), b.getAmount(),
                        walletRepository.balanceOf(b.getUserId()), "REFUND", b.getId()));
            }
        }
        return stuck.size();
    }

    private static LocalDateTime toLocal(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}