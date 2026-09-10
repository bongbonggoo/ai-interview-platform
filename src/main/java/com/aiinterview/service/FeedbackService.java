package com.aiinterview.service;

import com.aiinterview.dto.request.FeedbackRequest;
import com.aiinterview.dto.response.CompanyResponse;
import com.aiinterview.dto.response.CompetencyFeedbackResponse;
import com.aiinterview.dto.response.FeedbackResponse;
import com.aiinterview.entity.Company;
import com.aiinterview.entity.CompetencyEvidence;
import com.aiinterview.entity.CompetencyLibrary;
import com.aiinterview.repository.CompanyRepository;
import com.aiinterview.repository.CompetencyEvidenceRepository;
import com.aiinterview.repository.CompetencyLibraryRepository;
import com.aiinterview.service.scorer.CompetencyVerdict;
import com.aiinterview.service.scorer.FeedbackContext;
import com.aiinterview.service.scorer.FeedbackGenerator;
import com.aiinterview.service.scorer.FeedbackResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 이 서비스가 이 제품의 본체다.
 * 기관을 고르고 글을 붙여넣으면, 그 기관의 인재상을 기준으로 어디가 드러났고 어디가 약한지 짚어준다.
 *
 * 지키는 것:
 * - 기준은 그 기관의 인재상(출처 있는 것)에서만 나온다. 인재상을 확보 못 한 기관은
 *   공기업 공통 역량으로만 평가하고, 그 사실을 응답에 드러낸다.
 * - AI는 역량별 1~5점과 근거만 낸다. 총점은 여기서 계산한다(원칙 1).
 * - 모범답안은 만들지도, 저장하지도, 돌려주지도 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private static final int MIN_SCORE = 1;
    private static final int MAX_SCORE = 5;

    /** 인재상을 확보하지 못한 기관에 쓰는 공기업 공통 기준(NCS 직업기초능력 중 면접에서 관찰 가능한 것). */
    private static final List<String> COMMON_CRITERIA = List.of(
            "COMMUNICATION", "PROBLEM_SOLVING", "INTERPERSONAL",
            "ORGANIZATIONAL_UNDERSTANDING", "WORK_ETHIC");

    private final FeedbackGenerator feedbackGenerator;
    private final CompanyRepository companyRepository;
    private final CompetencyEvidenceRepository competencyEvidenceRepository;
    private final CompetencyLibraryRepository competencyLibraryRepository;

    public List<CompanyResponse> selectableCompanies() {
        return companyRepository.findAll().stream()
                .sorted(Comparator.comparing(Company::getName))
                .map(CompanyResponse::from)
                .toList();
    }

    public FeedbackResponse giveFeedback(FeedbackRequest request) {
        Company company = findCompany(request.companyName());
        List<CompetencyEvidence> evidences = competencyEvidenceRepository.findByCompanyId(company.getId());

        List<FeedbackContext.Criterion> criteria = evidences.isEmpty()
                ? commonCriteria()
                : evidences.stream().map(FeedbackService::toCriterion).toList();

        FeedbackContext context = new FeedbackContext(
                company.getName(),
                company.isCompetencyConfirmed(),
                request.documentTypeOrDefault(),
                request.question(),
                request.content(),
                criteria);

        FeedbackResult result = feedbackGenerator.generate(context);
        Map<String, CompetencyVerdict> byId = validateAndIndex(result, criteria);

        List<CompetencyFeedbackResponse> competencies = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();
        for (FeedbackContext.Criterion c : criteria) {
            CompetencyVerdict v = byId.get(c.canonicalId());
            scores.add(v.score());
            competencies.add(new CompetencyFeedbackResponse(
                    c.canonicalId(), c.companyLabel(), c.competencyLabel(), v.score(),
                    criterionTextFor(c, v.score()), v.strengths(), v.gaps(), v.quotedEvidence()));
        }

        CompetencyEvidence anySource = evidences.isEmpty() ? null : evidences.get(0);
        return new FeedbackResponse(
                company.getName(),
                company.isCompetencyConfirmed(),
                anySource == null ? null : anySource.getSourceUrl(),
                anySource == null ? null : anySource.getSourceTitle(),
                request.documentTypeOrDefault(),
                feedbackGenerator.name(),
                ScoreCalculator.equalWeightScore(scores),
                result.overall(),
                competencies);
    }

    private Company findCompany(String term) {
        return companyRepository.findByNameOrAlias(term.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "등록되지 않은 기관입니다: " + term
                                + " (GET /api/feedback/companies 로 목록을 확인하세요)"));
    }

    private List<FeedbackContext.Criterion> commonCriteria() {
        return COMMON_CRITERIA.stream()
                .map(id -> competencyLibraryRepository.findById(id).orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "공통 역량 시드가 없습니다: " + id)))
                .map(c -> new FeedbackContext.Criterion(
                        c.getCanonicalId(), c.getLabel(), c.getLabel(), c.getDefinition(),
                        c.getAnchor1(), c.getAnchor3(), c.getAnchor5()))
                .toList();
    }

    private static FeedbackContext.Criterion toCriterion(CompetencyEvidence evidence) {
        CompetencyLibrary c = evidence.getCompetency();
        return new FeedbackContext.Criterion(
                c.getCanonicalId(),
                evidence.getCompanyLabel(),
                c.getLabel(),
                evidence.getDescription() == null || evidence.getDescription().isBlank()
                        ? c.getDefinition() : evidence.getDescription(),
                c.getAnchor1(), c.getAnchor3(), c.getAnchor5());
    }

    /** 점수에 해당하는 행동기준. 2·4점은 인접한 기준 사이라는 것을 문구로 알려준다. */
    private static String criterionTextFor(FeedbackContext.Criterion c, int score) {
        return switch (score) {
            case 1 -> c.anchor1();
            case 2 -> "1점 기준(" + c.anchor1() + ")은 넘었으나 3점 기준에는 못 미침";
            case 3 -> c.anchor3();
            case 4 -> "3점 기준(" + c.anchor3() + ")은 넘었으나 5점 기준에는 못 미침";
            default -> c.anchor5();
        };
    }

    /** 생성기가 무엇이든 계약을 지켰는지 확인하고 나서야 응답으로 내보낸다. */
    private Map<String, CompetencyVerdict> validateAndIndex(FeedbackResult result,
                                                            List<FeedbackContext.Criterion> criteria) {
        if (result == null || result.verdicts() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "피드백 결과가 비어 있습니다");
        }
        Map<String, CompetencyVerdict> byId = new LinkedHashMap<>();
        for (CompetencyVerdict v : result.verdicts()) {
            if (v.score() == null || v.score() < MIN_SCORE || v.score() > MAX_SCORE) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "점수는 1~5여야 합니다. 받은 값: " + v.score() + " (" + v.canonicalId() + ")");
            }
            byId.put(v.canonicalId(), v);
        }
        List<String> missing = criteria.stream()
                .map(FeedbackContext.Criterion::canonicalId)
                .filter(id -> !byId.containsKey(id))
                .toList();
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "판정되지 않은 역량이 있습니다: " + missing);
        }
        return byId;
    }
}
