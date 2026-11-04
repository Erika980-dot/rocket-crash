package com.rocket.backend.game;

/** Published by RoundEngine whenever the round changes phase.  回合阶段改变时发布 */
public record RoundChangedEvent(String type, CurrentRound round) {}