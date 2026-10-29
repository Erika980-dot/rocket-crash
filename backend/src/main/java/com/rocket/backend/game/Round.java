package com.rocket.backend.game;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "rounds")
@Getter @Setter @NoArgsConstructor
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serverSeed;      // SECRET until crash  爆炸前保密
    private String seedHash;        // public from the start  一开始就公开
    private Integer crashPoint;     // 250 = 2.50x

    @Enumerated(EnumType.STRING)          // store the name "BETTING", not a number
    @JdbcTypeCode(SqlTypes.VARCHAR)       // our column is VARCHAR (not MySQL ENUM)
    private RoundStatus status;

    private LocalDateTime startedAt;      // launch time  起飞时间
    private LocalDateTime crashedAt;      // crash time   爆炸时间
}