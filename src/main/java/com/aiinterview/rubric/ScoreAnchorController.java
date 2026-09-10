package com.aiinterview.rubric;

import com.aiinterview.rubric.dto.ScoreAnchorRequest;
import com.aiinterview.rubric.dto.ScoreAnchorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/criteria/{criterionId}/anchors")
@RequiredArgsConstructor
public class ScoreAnchorController {

    private final ScoreAnchorService scoreAnchorService;

    /** level별 멱등 upsert — 같은 level을 다시 보내면 문구만 바뀐다. */
    @PutMapping
    public ScoreAnchorResponse upsert(@PathVariable UUID criterionId,
                                      @Valid @RequestBody ScoreAnchorRequest request) {
        return scoreAnchorService.upsert(criterionId, request);
    }

    @GetMapping
    public List<ScoreAnchorResponse> getByCriterion(@PathVariable UUID criterionId) {
        return scoreAnchorService.getByCriterion(criterionId);
    }
}
