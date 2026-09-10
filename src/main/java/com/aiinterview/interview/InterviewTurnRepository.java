package com.aiinterview.interview;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewTurnRepository extends JpaRepository<InterviewTurn, UUID> {
    List<InterviewTurn> findBySessionIdOrderByOrderIndexAsc(UUID sessionId);
    Optional<InterviewTurn> findTopBySessionIdOrderByOrderIndexDesc(UUID sessionId);
}
