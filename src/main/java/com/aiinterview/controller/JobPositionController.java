package com.aiinterview.controller;

import com.aiinterview.dto.request.JobPositionCreateRequest;
import com.aiinterview.dto.response.JobPositionResponse;
import com.aiinterview.service.JobPositionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies/{companyId}/job-positions")
@RequiredArgsConstructor
public class JobPositionController {

    private final JobPositionService jobPositionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobPositionResponse create(@PathVariable UUID companyId,
                                      @Valid @RequestBody JobPositionCreateRequest request) {
        return jobPositionService.create(companyId, request);
    }

    @GetMapping
    public List<JobPositionResponse> getByCompany(@PathVariable UUID companyId) {
        return jobPositionService.getByCompany(companyId);
    }
}
