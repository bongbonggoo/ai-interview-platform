package com.aiinterview.service;

import com.aiinterview.dto.request.ScoreAnchorRequest;
import com.aiinterview.dto.response.ScoreAnchorResponse;
import com.aiinterview.entity.EvaluationCriterion;
import com.aiinterview.entity.ScoreAnchor;
import com.aiinterview.repository.EvaluationCriterionRepository;
import com.aiinterview.repository.ScoreAnchorRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 앵커는 criterion에 딸린 1/3/5 세 줄이 전부다. 같은 level을 두 번 넣으면 새로 만들지 않고 문구를 갈아끼운다.
 * EvaluationCriterionService가 이 서비스를 쓰므로, 여기서는 리포지토리만 보고 역방향 의존을 만들지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScoreAnchorService {

    private static final Set<Integer> ALLOWED_LEVELS = Set.of(1, 3, 5);

    private final ScoreAnchorRepository scoreAnchorRepository;
    private final EvaluationCriterionRepository evaluationCriterionRepository;
    private final RubricVersionService rubricVersionService;

    @Transactional
    public ScoreAnchorResponse upsert(UUID criterionId, ScoreAnchorRequest request) {
        return ScoreAnchorResponse.from(upsert(findCriterion(criterionId), request));
    }

    @Transactional
    public ScoreAnchor upsert(EvaluationCriterion criterion, ScoreAnchorRequest request) {
        rubricVersionService.assertMutable(criterion.getRubricVersion());

        if (!ALLOWED_LEVELS.contains(request.level())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "행동기준 level은 1, 3, 5만 허용됩니다. 요청: " + request.level());
        }

        ScoreAnchor anchor = scoreAnchorRepository
                .findByCriterionIdAndLevel(criterion.getId(), request.level())
                .orElseGet(() -> ScoreAnchor.builder()
                        .criterion(criterion)
                        .level(request.level())
                        .build());
        anchor.setDescription(request.description());

        return scoreAnchorRepository.save(anchor);
    }

    public List<ScoreAnchorResponse> getByCriterion(UUID criterionId) {
        findCriterion(criterionId);
        return scoreAnchorRepository.findByCriterionIdOrderByLevelAsc(criterionId).stream()
                .map(ScoreAnchorResponse::from)
                .toList();
    }

    private EvaluationCriterion findCriterion(UUID criterionId) {
        return evaluationCriterionRepository.findById(criterionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "평가 기준을 찾을 수 없습니다: " + criterionId));
    }
}
