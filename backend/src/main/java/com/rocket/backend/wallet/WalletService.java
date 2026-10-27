package com.rocket.backend.wallet;

import com.rocket.backend.common.GameException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletResponse getWallet(Long userId) {
        Wallet wallet = walletRepository.findById(userId)
                .orElseThrow(() -> new GameException(
                        HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", "No wallet for user " + userId));
        return new WalletResponse(wallet.getUserId(), wallet.getBalance());
    }
}
