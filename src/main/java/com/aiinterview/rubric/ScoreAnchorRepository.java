package com.aiinterview.rubric;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScoreAnchorRepository extends JpaRepository<ScoreAnchor, UUID> {
    List<ScoreAnchor> findByCriterionIdOrderByLevelAsc(UUID criterionId);
    Optional<ScoreAnchor> findByCriterionIdAndLevel(UUID criterionId, Integer level);
}
