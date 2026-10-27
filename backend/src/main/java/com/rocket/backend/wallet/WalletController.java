package com.rocket.backend.wallet;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/{userId}")
    public WalletResponse get(@PathVariable Long userId) {
        return walletService.getWallet(userId);
    }
}
