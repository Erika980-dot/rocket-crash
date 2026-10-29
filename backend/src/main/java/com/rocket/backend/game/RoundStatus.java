package com.rocket.backend.game;

public enum RoundStatus {
    BETTING,    // 下注中
    RUNNING,    // 飞行中
    CRASHED,    // 已爆炸
    CANCELLED   // 已取消(服务器中途重启)
}