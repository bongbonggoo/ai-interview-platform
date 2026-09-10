package com.aiinterview.interview;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, UUID> {
    Optional<InterviewAnswer> findByTurnId(UUID turnId);
}
