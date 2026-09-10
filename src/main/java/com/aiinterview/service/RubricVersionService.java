package com.aiinterview.service;

import com.aiinterview.dto.request.RubricVersionCreateRequest;
import com.aiinterview.dto.response.RubricValidationResponse;
import com.aiinterview.dto.response.RubricVersionResponse;
import com.aiinterview.entity.Company;
import com.aiinterview.entity.EvaluationCriterion;
import com.aiinterview.entity.RubricVersion;
import com.aiinterview.entity.ScoreAnchor;
import com.aiinterview.repository.CompanyRepository;
import com.aiinterview.repository.EvaluationCriterionRepository;
import com.aiinterview.repository.RubricVersionRepository;
import com.aiinterview.repository.ScoreAnchorRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * CLAUDE.md 원칙 3의 집행 지점.
 * - 앵커 문구를 고치고 싶으면 이 버전을 수정하는 게 아니라 새 버전을 만든다.
 * - 그래서 verified된 버전은 이 서비스 아래에서 더 이상 변경되지 않는다(하위 criterion/anchor 포함).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RubricVersionService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_VERIFIED = "VERIFIED";
    private static final Set<Integer> REQUIRED_ANCHOR_LEVELS = Set.of(1, 3, 5);
    private static final int REQUIRED_WEIGHT_SUM = 100;

    private final RubricVersionRepository rubricVersionRepository;
    private final CompanyRepository companyRepository;
    private final EvaluationCriterionRepository evaluationCriterionRepository;
    private final ScoreAnchorRepository scoreAnchorRepository;

    @Transactional
    public RubricVersionResponse create(UUID companyId, RubricVersionCreateRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회사를 찾을 수 없습니다: " + companyId));

        int nextVersionNumber = rubricVersionRepository.findByCompanyIdOrderByVersionNumberDesc(companyId).stream()
                .findFirst()
                .map(latest -> latest.getVersionNumber() + 1)
                .orElse(1);

        RubricVersion rubricVersion = RubricVersion.builder()
                .company(company)
                .versionNumber(nextVersionNumber)
                .status(STATUS_DRAFT)
                .changelog(request == null ? null : request.changelog())
                .build();

        return RubricVersionResponse.from(rubricVersionRepository.saveAndFlush(rubricVersion));
    }

    public List<RubricVersionResponse> getByCompany(UUID companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회사를 찾을 수 없습니다: " + companyId);
        }
        return rubricVersionRepository.findByCompanyIdOrderByVersionNumberDesc(companyId).stream()
                .map(RubricVersionResponse::from)
                .toList();
    }

    public RubricVersionResponse getById(UUID id) {
        return RubricVersionResponse.from(getEntity(id));
    }

    public RubricVersion getEntity(UUID id) {
        return rubricVersionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "루브릭 버전을 찾을 수 없습니다: " + id));
    }

    /** 확정된 버전은 더 이상 손대지 않는다. 하위 criterion/anchor를 바꾸려는 모든 경로가 여기를 지난다. */
    void assertMutable(RubricVersion rubricVersion) {
        if (STATUS_VERIFIED.equals(rubricVersion.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "이미 verified된 루브릭 버전 v" + rubricVersion.getVersionNumber() + "은 수정할 수 없습니다. "
                            + "새 버전을 만들어서 바꾸세요 (POST /companies/{companyId}/rubric-versions)");
        }
    }

    public RubricValidationResponse validate(UUID rubricVersionId) {
        RubricVersion rubricVersion = getEntity(rubricVersionId);
        List<String> problems = collectProblems(rubricVersion);
        return new RubricValidationResponse(rubricVersion.getId(), problems.isEmpty(), problems);
    }

    /**
     * draft -> verified. 통과 조건:
     * 1) criterion이 하나 이상 있고
     * 2) 질문별 weight_pct 합이 정확히 100이고
     * 3) 모든 criterion에 1/3/5 앵커가 다 있다.
     * 회사 status도 이때 verified로 올라간다("Verified 루브릭을 보유한 회사"의 정의).
     */
    @Transactional
    public RubricVersionResponse verify(UUID rubricVersionId) {
        RubricVersion rubricVersion = getEntity(rubricVersionId);
        if (STATUS_VERIFIED.equals(rubricVersion.getStatus())) {
            return RubricVersionResponse.from(rubricVersion);
        }

        List<String> problems = collectProblems(rubricVersion);
        if (!problems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "루브릭을 verified로 올릴 수 없습니다: " + String.join(" / ", problems));
        }

        rubricVersion.setStatus(STATUS_VERIFIED);
        rubricVersion.getCompany().setStatus(STATUS_VERIFIED);
        return RubricVersionResponse.from(rubricVersion);
    }

    private List<String> collectProblems(RubricVersion rubricVersion) {
        List<String> problems = new ArrayList<>();
        List<EvaluationCriterion> criteria = evaluationCriterionRepository.findByRubricVersionId(rubricVersion.getId());

        if (criteria.isEmpty()) {
            problems.add("평가 기준(EvaluationCriterion)이 하나도 없습니다");
            return problems;
        }

        Map<UUID, Integer> weightSumByQuestion = new LinkedHashMap<>();
        for (EvaluationCriterion criterion : criteria) {
            weightSumByQuestion.merge(criterion.getQuestion().getId(), criterion.getWeightPct(), Integer::sum);

            Set<Integer> levels = scoreAnchorRepository.findByCriterionIdOrderByLevelAsc(criterion.getId()).stream()
                    .map(ScoreAnchor::getLevel)
                    .collect(java.util.stream.Collectors.toSet());
            if (!levels.containsAll(REQUIRED_ANCHOR_LEVELS)) {
                problems.add("criterion " + criterion.getId() + "(" + criterion.getCompetency().getCanonicalId()
                        + ")에 1/3/5 행동기준이 모두 있지 않습니다. 현재: " + levels);
            }
        }

        weightSumByQuestion.forEach((questionId, sum) -> {
            if (sum != REQUIRED_WEIGHT_SUM) {
                problems.add("질문 " + questionId + "의 weight_pct 합이 " + sum + "입니다 (100이어야 함)");
            }
        });

        return problems;
    }
}
