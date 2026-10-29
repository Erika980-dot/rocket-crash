package com.rocket.backend.game;

import com.rocket.backend.common.GameException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rounds")
@RequiredArgsConstructor
public class RoundController {

    private final RoundEngine roundEngine;

    @GetMapping("/current")
    public RoundView current() {
        CurrentRound c = roundEngine.current();
        if (c == null) {
            throw new GameException(HttpStatus.SERVICE_UNAVAILABLE, "NO_ROUND", "Game is starting");
        }
        return RoundView.from(c);
    }
}