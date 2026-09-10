package com.aiinterview.rubric;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScoreAnchorRepository extends JpaRepository<ScoreAnchor, UUID> {
    List<ScoreAnchor> findByCriterionIdOrderByLevelAsc(UUID criterionId);
}
