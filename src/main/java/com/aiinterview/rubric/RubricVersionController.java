package com.aiinterview.rubric;

import com.aiinterview.rubric.dto.RubricValidationResponse;
import com.aiinterview.rubric.dto.RubricVersionCreateRequest;
import com.aiinterview.rubric.dto.RubricVersionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RubricVersionController {

    private final RubricVersionService rubricVersionService;

    @PostMapping("/companies/{companyId}/rubric-versions")
    @ResponseStatus(HttpStatus.CREATED)
    public RubricVersionResponse create(@PathVariable UUID companyId,
                                        @RequestBody(required = false) RubricVersionCreateRequest request) {
        return rubricVersionService.create(companyId, request);
    }

    @GetMapping("/companies/{companyId}/rubric-versions")
    public List<RubricVersionResponse> getByCompany(@PathVariable UUID companyId) {
        return rubricVersionService.getByCompany(companyId);
    }

    @GetMapping("/rubric-versions/{id}")
    public RubricVersionResponse getById(@PathVariable UUID id) {
        return rubricVersionService.getById(id);
    }

    /** 올리기 전에 무엇이 막고 있는지 미리 본다(상태는 바뀌지 않음). */
    @GetMapping("/rubric-versions/{id}/validation")
    public RubricValidationResponse validate(@PathVariable UUID id) {
        return rubricVersionService.validate(id);
    }

    @PostMapping("/rubric-versions/{id}/verify")
    public RubricVersionResponse verify(@PathVariable UUID id) {
        return rubricVersionService.verify(id);
    }
}
