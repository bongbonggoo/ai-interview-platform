package com.aiinterview.controller;

import com.aiinterview.dto.request.CompetencyUpsertRequest;
import com.aiinterview.dto.response.CompetencyResponse;
import com.aiinterview.service.CompetencyLibraryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/competencies")
@RequiredArgsConstructor
public class CompetencyLibraryController {

    private final CompetencyLibraryService competencyLibraryService;

    @PutMapping("/{canonicalId}")
    public CompetencyResponse upsert(@PathVariable String canonicalId,
                                     @Valid @RequestBody CompetencyUpsertRequest request) {
        return competencyLibraryService.upsert(canonicalId, request);
    }

    @GetMapping
    public List<CompetencyResponse> getAll() {
        return competencyLibraryService.getAll();
    }

    @GetMapping("/{canonicalId}")
    public CompetencyResponse getById(@PathVariable String canonicalId) {
        return competencyLibraryService.getById(canonicalId);
    }
}
