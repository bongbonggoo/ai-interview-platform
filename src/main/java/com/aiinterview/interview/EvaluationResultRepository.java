package com.aiinterview.interview;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, UUID> {
    List<EvaluationResult> findByAnswerId(UUID answerId);
    boolean existsByAnswerId(UUID answerId);
    List<EvaluationResult> findByAnswerTurnSessionId(UUID sessionId);
}
