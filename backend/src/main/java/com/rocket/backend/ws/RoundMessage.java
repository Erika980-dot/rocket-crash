package com.rocket.backend.ws;

import com.rocket.backend.game.RoundView;

public record RoundMessage(String type, RoundView round) {}