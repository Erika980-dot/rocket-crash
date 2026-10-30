CREATE TABLE users (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  username      VARCHAR(32) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE wallets (
  user_id  BIGINT PRIMARY KEY,
  balance  BIGINT NOT NULL DEFAULT 0,      -- points, never negative
  version  INT NOT NULL DEFAULT 0,         -- for optimistic locking
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE rounds (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  server_seed CHAR(64) NOT NULL,           -- secret until round ends
  seed_hash   CHAR(64) NOT NULL,           -- shown to players before round
  crash_point INT NOT NULL,                -- 250 means 2.50x
  status      VARCHAR(16) NOT NULL,        -- BETTING, RUNNING, CRASHED
  started_at  DATETIME,
  crashed_at  DATETIME
);

CREATE TABLE bets (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  round_id        BIGINT NOT NULL,
  user_id         BIGINT NOT NULL,
  amount          BIGINT NOT NULL,
  cashout_at      INT NULL,                -- NULL = did not cash out
  payout          BIGINT NOT NULL DEFAULT 0,
  status          VARCHAR(16) NOT NULL,    -- PLACED, WON, LOST
  request_id      VARCHAR(64) NOT NULL,    -- idempotency key
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_round_user (round_id, user_id),  -- one bet per round
  UNIQUE KEY uk_request (request_id),
  KEY idx_user_time (user_id, created_at)        -- for "my history"
);

CREATE TABLE ledger (                      -- every balance change, forever
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT NOT NULL,
  change_amount BIGINT NOT NULL,           -- -100 for bet, +250 for win
  balance_after BIGINT NOT NULL,
  reason        VARCHAR(32) NOT NULL,      -- BET, PAYOUT, SIGNUP_BONUS
  ref_id        BIGINT,                    -- the bet id
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user_time (user_id, created_at)
);

CREATE TABLE user_sessions (
  id           VARCHAR(36) PRIMARY KEY,     -- the session id ("sid"), a UUID
  user_id      BIGINT NOT NULL,
  refresh_hash VARCHAR(64) NOT NULL,        -- SHA-256 of the refresh token (64 hex chars)
  expires_at   DATETIME NOT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user (user_id),             -- ONE session per user = duplicate-login rule
  UNIQUE KEY uk_refresh (refresh_hash)      -- fast lookup by token
);

ALTER TABLE rounds
  MODIFY server_seed VARCHAR(64) NOT NULL,
  MODIFY seed_hash   VARCHAR(64) NOT NULL,
  MODIFY started_at  DATETIME(3) NULL,
  MODIFY crashed_at  DATETIME(3) NULL;