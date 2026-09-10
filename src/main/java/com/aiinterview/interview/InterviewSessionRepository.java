package com.aiinterview.interview;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, UUID> {
    List<InterviewSession> findByUserIdentifier(String userIdentifier);
}
