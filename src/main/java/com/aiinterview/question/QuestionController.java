package com.aiinterview.question;

import com.aiinterview.question.dto.QuestionCreateRequest;
import com.aiinterview.question.dto.QuestionResponse;
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

    @PostMapping("/companies/{companyId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse create(@PathVariable UUID companyId,
                                   @Valid @RequestBody QuestionCreateRequest request) {
        return questionService.create(companyId, request);
    }

    @GetMapping("/companies/{companyId}/questions")
    public List<QuestionResponse> getByCompany(@PathVariable UUID companyId) {
        return questionService.getByCompany(companyId);
    }

    @GetMapping("/questions/{id}")
    public QuestionResponse getById(@PathVariable UUID id) {
        return questionService.getById(id);
    }
}
