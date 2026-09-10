package com.aiinterview.rubric;

import com.aiinterview.rubric.dto.EvaluationCriterionCreateRequest;
import com.aiinterview.rubric.dto.EvaluationCriterionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class EvaluationCriterionController {

    private final EvaluationCriterionService evaluationCriterionService;

    @PostMapping("/rubric-versions/{rubricVersionId}/criteria")
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluationCriterionResponse create(@PathVariable UUID rubricVersionId,
                                              @Valid @RequestBody EvaluationCriterionCreateRequest request) {
        return evaluationCriterionService.create(rubricVersionId, request);
    }

    @GetMapping("/rubric-versions/{rubricVersionId}/criteria")
    public List<EvaluationCriterionResponse> getByRubricVersion(@PathVariable UUID rubricVersionId) {
        return evaluationCriterionService.getByRubricVersion(rubricVersionId);
    }

    @GetMapping("/criteria/{id}")
    public EvaluationCriterionResponse getById(@PathVariable UUID id) {
        return evaluationCriterionService.getById(id);
    }
}
