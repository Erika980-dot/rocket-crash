package com.rocket.backend.bet;

public enum BetStatus {
    PLACED,     // 已下注,还在飞
    WON,        // 已提现,赢了
    LOST,       // 爆炸前没提现,输了
    REFUNDED    // 回合被取消,已退款
}