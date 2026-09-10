package com.aiinterview.controller;

import com.aiinterview.dto.request.RubricVersionCreateRequest;
import com.aiinterview.dto.response.RubricValidationResponse;
import com.aiinterview.dto.response.RubricVersionResponse;
import com.aiinterview.service.RubricVersionService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RubricVersionController {

    private final RubricVersionService rubricVersionService;

    @PostMapping("/api/companies/{companyId}/rubric-versions")
    @ResponseStatus(HttpStatus.CREATED)
    public RubricVersionResponse create(@PathVariable UUID companyId,
                                        @RequestBody(required = false) RubricVersionCreateRequest request) {
        return rubricVersionService.create(companyId, request);
    }

    @GetMapping("/api/companies/{companyId}/rubric-versions")
    public List<RubricVersionResponse> getByCompany(@PathVariable UUID companyId) {
        return rubricVersionService.getByCompany(companyId);
    }

    @GetMapping("/api/rubric-versions/{id}")
    public RubricVersionResponse getById(@PathVariable UUID id) {
        return rubricVersionService.getById(id);
    }

    /** 올리기 전에 무엇이 막고 있는지 미리 본다(상태는 바뀌지 않음). */
    @GetMapping("/api/rubric-versions/{id}/validation")
    public RubricValidationResponse validate(@PathVariable UUID id) {
        return rubricVersionService.validate(id);
    }

    @PostMapping("/api/rubric-versions/{id}/verify")
    public RubricVersionResponse verify(@PathVariable UUID id) {
        return rubricVersionService.verify(id);
    }
}
