package com.aiinterview.controller;

import com.aiinterview.dto.request.QuestionCreateRequest;
import com.aiinterview.dto.response.QuestionResponse;
import com.aiinterview.service.QuestionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping("/api/companies/{companyId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse create(@PathVariable UUID companyId,
                                   @Valid @RequestBody QuestionCreateRequest request) {
        return questionService.create(companyId, request);
    }

    @GetMapping("/api/companies/{companyId}/questions")
    public List<QuestionResponse> getByCompany(@PathVariable UUID companyId) {
        return questionService.getByCompany(companyId);
    }

    @GetMapping("/api/questions/{id}")
    public QuestionResponse getById(@PathVariable UUID id) {
        return questionService.getById(id);
    }
}
