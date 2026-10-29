package com.rocket.backend.bet;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bets")
@RequiredArgsConstructor
public class BetController {

    private final BetService betService;

    @PostMapping
    public BetResponse place(@AuthenticationPrincipal Long userId,
                             @Valid @RequestBody PlaceBetRequest req) {
        return betService.placeBet(userId, req);
    }

    @PostMapping("/cashout")
    public BetResponse cashOut(@AuthenticationPrincipal Long userId) {   // no body!  没有请求体
        return betService.cashOut(userId);
    }

    @GetMapping("/me")
    public List<BetResponse> history(@AuthenticationPrincipal Long userId) {
        return betService.myHistory(userId);
    }
}