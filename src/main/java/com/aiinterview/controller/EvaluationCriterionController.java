package com.aiinterview.controller;

import com.aiinterview.dto.request.EvaluationCriterionCreateRequest;
import com.aiinterview.dto.response.EvaluationCriterionResponse;
import com.aiinterview.service.EvaluationCriterionService;

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

    @PostMapping("/api/rubric-versions/{rubricVersionId}/criteria")
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluationCriterionResponse create(@PathVariable UUID rubricVersionId,
                                              @Valid @RequestBody EvaluationCriterionCreateRequest request) {
        return evaluationCriterionService.create(rubricVersionId, request);
    }

    @GetMapping("/api/rubric-versions/{rubricVersionId}/criteria")
    public List<EvaluationCriterionResponse> getByRubricVersion(@PathVariable UUID rubricVersionId) {
        return evaluationCriterionService.getByRubricVersion(rubricVersionId);
    }

    @GetMapping("/api/criteria/{id}")
    public EvaluationCriterionResponse getById(@PathVariable UUID id) {
        return evaluationCriterionService.getById(id);
    }
}
