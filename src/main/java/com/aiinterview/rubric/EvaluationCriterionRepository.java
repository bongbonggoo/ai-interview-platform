package com.aiinterview.rubric;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EvaluationCriterionRepository extends JpaRepository<EvaluationCriterion, UUID> {
    List<EvaluationCriterion> findByQuestionId(UUID questionId);
    List<EvaluationCriterion> findByRubricVersionId(UUID rubricVersionId);
    List<EvaluationCriterion> findByRubricVersionIdAndQuestionId(UUID rubricVersionId, UUID questionId);
    boolean existsByRubricVersionIdAndQuestionIdAndCompetencyCanonicalId(
            UUID rubricVersionId, UUID questionId, String canonicalId);
}
