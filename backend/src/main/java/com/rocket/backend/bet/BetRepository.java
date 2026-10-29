package com.rocket.backend.bet;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BetRepository extends JpaRepository<Bet, Long> {

    Optional<Bet> findByRequestId(String requestId);

    boolean existsByRoundIdAndUserId(Long roundId, Long userId);

    List<Bet> findByRoundIdAndStatus(Long roundId, BetStatus status);

    /** My last 20 bets, newest first. Uses idx_user_time from Step 1!  使用 Step 1 建的索引 */
    List<Bet> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

    /** SELECT ... FOR UPDATE: lock this bet row until our transaction ends.  锁住这一行 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Bet b WHERE b.roundId = :roundId AND b.userId = :userId")
    Optional<Bet> findForUpdate(@Param("roundId") Long roundId, @Param("userId") Long userId);

    /** At crash: everyone still PLACED becomes LOST.  爆炸时:还没提现的全部改成输 */
    @Modifying
    @Query("UPDATE Bet b SET b.status = :lost WHERE b.roundId = :roundId AND b.status = :placed")
    int settleLosers(@Param("roundId") Long roundId,
                     @Param("placed") BetStatus placed,
                     @Param("lost") BetStatus lost);
}