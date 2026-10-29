package com.rocket.backend.bet;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "bets")
@Getter @Setter @NoArgsConstructor
public class Bet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long roundId;
    private Long userId;
    private Long amount;
    private Integer cashoutAt;      // null = did not cash out   没提现就是 null
    private Long payout;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private BetStatus status;

    private String requestId;       // idempotency key   幂等键

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Bet placed(Long roundId, Long userId, long amount, String requestId) {
        Bet b = new Bet();
        b.roundId = roundId;
        b.userId = userId;
        b.amount = amount;
        b.payout = 0L;
        b.status = BetStatus.PLACED;
        b.requestId = requestId;
        return b;
    }
}