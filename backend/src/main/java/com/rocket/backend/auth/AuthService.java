package com.rocket.backend.auth;

import com.rocket.backend.common.GameException;
import com.rocket.backend.ledger.LedgerEntry;
import com.rocket.backend.ledger.LedgerRepository;
import com.rocket.backend.session.SessionService;
import com.rocket.backend.session.SessionTokens;
import com.rocket.backend.user.User;
import com.rocket.backend.user.UserRepository;
import com.rocket.backend.wallet.Wallet;
import com.rocket.backend.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long SIGNUP_BONUS = 1000;

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SessionService sessionService;

    @Transactional   // user + wallet + ledger: all three saved, or none (Step 1, Experiment 4!)
    public RegisterResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new GameException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "Username already taken");
        }

        User user = new User();
        user.setUsername(req.username());
        user.setPasswordHash(passwordEncoder.encode(req.password()));  // hash, never plain
        userRepository.save(user);   // MySQL assigns the id; user.getId() now has it

        Wallet wallet = new Wallet();
        wallet.setUserId(user.getId());
        wallet.setBalance(SIGNUP_BONUS);
        walletRepository.save(wallet);

        ledgerRepository.save(
                LedgerEntry.of(user.getId(), SIGNUP_BONUS, SIGNUP_BONUS, "SIGNUP_BONUS", null));

        return new RegisterResponse(user.getId(), user.getUsername(), SIGNUP_BONUS);
    }

    public TokenResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.username())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new GameException(
                        HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Wrong username or password"));

        SessionTokens session = sessionService.start(user.getId());   // kicks any old device
        return toTokenResponse(session);
    }

    public TokenResponse refresh(RefreshRequest req) {
        SessionTokens session = sessionService.rotate(req.refreshToken());
        return toTokenResponse(session);
    }

    public void logout(RefreshRequest req) {
        sessionService.logout(req.refreshToken());
    }

    private TokenResponse toTokenResponse(SessionTokens s) {
        String access = jwtService.createAccessToken(s.userId(), s.sessionId());
        return new TokenResponse(access, s.refreshToken(), "Bearer", jwtService.accessTtlSeconds());
    }
}