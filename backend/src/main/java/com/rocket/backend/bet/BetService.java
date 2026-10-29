package com.rocket.backend.bet;

import com.rocket.backend.common.GameException;
import com.rocket.backend.game.CurrentRound;
import com.rocket.backend.game.RoundEngine;
import com.rocket.backend.game.RoundStatus;
import com.rocket.backend.ledger.LedgerEntry;
import com.rocket.backend.ledger.LedgerRepository;
import com.rocket.backend.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BetService {

    private final BetRepository betRepository;
    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;
    private final RoundEngine roundEngine;

    // ================= PLACE BET 下注 =================
    @Transactional
    public BetResponse placeBet(Long userId, PlaceBetRequest req) {

        // 1. Idempotency: same requestId again? Return the original bet, charge nothing.
        //    幂等:同一个 requestId 再来一次?直接返回原来的注单,不再扣钱。
        var existing = betRepository.findByRequestId(req.requestId());
        if (existing.isPresent()) {
            Bet b = existing.get();
            if (!b.getUserId().equals(userId)) {
                throw new GameException(HttpStatus.CONFLICT, "REQUEST_ID_USED", "Use a new requestId");
            }
            return BetResponse.from(b);
        }

        // 2. Betting must be open.  必须在下注阶段
        CurrentRound round = roundEngine.current();
        if (round == null || round.status() != RoundStatus.BETTING) {
            throw new GameException(HttpStatus.CONFLICT, "BETTING_CLOSED", "Wait for the next round");
        }

        // 3. One bet per round (friendly message; the DB unique key is the real guarantee).
        //    每局只能下一注(友善提示;真正的保证是数据库唯一键)
        if (betRepository.existsByRoundIdAndUserId(round.id(), userId)) {
            throw new GameException(HttpStatus.CONFLICT, "ALREADY_BET", "You already bet this round");
        }

        // 4. Take the money (atomic check + debit).  扣钱(检查和扣款一步完成)
        if (walletRepository.debit(userId, req.amount()) == 0) {
            throw new GameException(HttpStatus.BAD_REQUEST, "INSUFFICIENT_BALANCE", "Not enough points");
        }

        // 5. Record the bet + the ledger line.  记录注单和账本
        Bet bet = betRepository.save(Bet.placed(round.id(), userId, req.amount(), req.requestId()));
        ledgerRepository.save(LedgerEntry.of(
                userId, -req.amount(), walletRepository.balanceOf(userId), "BET", bet.getId()));

        return BetResponse.from(bet);
        // If ANY line above throws, @Transactional rolls back EVERYTHING, including the debit.
        // 上面任何一行出错,整个事务回滚,连扣掉的钱也会退回。
    }

    // ================= CASH OUT 提现 =================
    @Transactional
    public BetResponse cashOut(Long userId) {

        // 1. The rocket must be flying.  火箭必须在飞行中
        CurrentRound round = roundEngine.current();
        if (round == null || round.status() != RoundStatus.RUNNING) {
            throw new GameException(HttpStatus.CONFLICT, "NOT_RUNNING", "Rocket is not flying");
        }

        // 2. Lock MY bet row (a second click waits here).  锁住我的注单(第二次点击会在这里等)
        Bet bet = betRepository.findForUpdate(round.id(), userId)
                .orElseThrow(() -> new GameException(HttpStatus.NOT_FOUND, "NO_BET", "You have no bet this round"));

        // 3. Already cashed out, or already LOST by the crash?  已经提现过,或已经被判输?
        if (bet.getStatus() != BetStatus.PLACED) {
            throw new GameException(HttpStatus.CONFLICT, "ALREADY_SETTLED", "This bet is already settled");
        }

        // 4. The SERVER decides the multiplier, from its own clock.  倍数由服务器的时钟决定
        int multiplier = round.multiplierNow();
        if (multiplier >= round.crashPoint()) {
            throw new GameException(HttpStatus.CONFLICT, "TOO_LATE", "The rocket already crashed");
        }

        // 5. Pay out.  派奖
        long payout = bet.getAmount() * multiplier / 100;    // 100 × 250 / 100 = 250
        bet.setStatus(BetStatus.WON);
        bet.setCashoutAt(multiplier);
        bet.setPayout(payout);                               // saved by dirty checking  自动保存
        walletRepository.credit(userId, payout);
        ledgerRepository.save(LedgerEntry.of(
                userId, payout, walletRepository.balanceOf(userId), "PAYOUT", bet.getId()));

        return BetResponse.from(bet);
    }

    // ================= HISTORY 下注记录 =================
    public List<BetResponse> myHistory(Long userId) {
        return betRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(BetResponse::from).toList();
    }
}