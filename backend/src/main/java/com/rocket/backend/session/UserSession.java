package com.rocket.backend.session;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
@Getter @Setter @NoArgsConstructor
public class UserSession {

    @Id
    private String id;              // we create the UUID ourselves, so no @GeneratedValue

    private Long userId;
    private String refreshHash;
    private LocalDateTime expiresAt;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;
}