package com.aiinterview.competency;

import com.aiinterview.competency.dto.CompetencyResponse;
import com.aiinterview.competency.dto.CompetencyUpsertRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/competencies")
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
