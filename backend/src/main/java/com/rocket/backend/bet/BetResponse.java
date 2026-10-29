package com.rocket.backend.bet;

public record BetResponse(Long id, Long roundId, Long amount, BetStatus status,
                          Integer cashoutAt, Long payout) {
    static BetResponse from(Bet b) {
        return new BetResponse(b.getId(), b.getRoundId(), b.getAmount(), b.getStatus(),
                b.getCashoutAt(), b.getPayout());
    }
}