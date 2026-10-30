package com.rocket.backend.game;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RoundRepository extends JpaRepository<Round, Long> {
    List<Round> findByStatusIn(Collection<RoundStatus> statuses);   // WHERE status IN (...)
    List<Round> findTop10ByStatusOrderByIdDesc(RoundStatus status);
// → SELECT * FROM rounds WHERE status = ? ORDER BY id DESC LIMIT 10
}