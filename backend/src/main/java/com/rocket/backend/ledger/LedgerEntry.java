package com.rocket.backend.ledger;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ledger")
@Getter @Setter @NoArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long changeAmount;
    private Long balanceAfter;
    private String reason;
    private Long refId;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // A "factory method": a one-line way to build a ledger row
    public static LedgerEntry of(Long userId, long change, long balanceAfter, String reason, Long refId) {
        LedgerEntry e = new LedgerEntry();
        e.userId = userId;
        e.changeAmount = change;
        e.balanceAfter = balanceAfter;
        e.reason = reason;
        e.refId = refId;
        return e;
    }
}