package com.rocket.backend.wallet;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity@Table(name = "wallets")
@Getter @Setter
public class Wallet {

    @Id
    private Long userId;

    private Long balance;

    @Version
    private Integer version;
}
