package com.rocket.backend.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /** Returns 1 if debited, 0 if balance too low.  成功返回 1,余额不够返回 0 */
    @Modifying
    @Query("UPDATE Wallet w SET w.balance = w.balance - :amount " +
            "WHERE w.userId = :userId AND w.balance >= :amount")
    int debit(@Param("userId") Long userId, @Param("amount") long amount);

    @Modifying
    @Query("UPDATE Wallet w SET w.balance = w.balance + :amount WHERE w.userId = :userId")
    int credit(@Param("userId") Long userId, @Param("amount") long amount);

    /** Fresh balance straight from the DB (for the ledger's balance_after).  直接从数据库读最新余额 */
    @Query("SELECT w.balance FROM Wallet w WHERE w.userId = :userId")
    long balanceOf(@Param("userId") Long userId);
}