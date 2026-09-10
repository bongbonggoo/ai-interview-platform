package com.aiinterview.service;

import com.aiinterview.dto.request.QuestionGenerateRequest;
import com.aiinterview.dto.response.GeneratedQuestionResponse;
import com.aiinterview.dto.response.QuestionGenerateResponse;
import com.aiinterview.entity.Company;
import com.aiinterview.entity.CompetencyEvidence;
import com.aiinterview.entity.CompetencyLibrary;
import com.aiinterview.entity.Question;
import com.aiinterview.repository.CompanyRepository;
import com.aiinterview.repository.CompetencyEvidenceRepository;
import com.aiinterview.repository.CompetencyLibraryRepository;
import com.aiinterview.repository.QuestionRepository;
import com.aiinterview.service.scorer.GeneratedQuestion;
import com.aiinterview.service.scorer.QuestionContext;
import com.aiinterview.service.scorer.QuestionGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 기관을 고르면 그 기관 인재상에 맞는 면접 질문을 만들어준다.
 *
 * 질문은 생성만 하고 끝내지 않고 Question으로 저장한다 — 그래야 그 질문으로 면접 세션을 열고,
 * 답변을 받아 같은 역량 기준으로 채점하는 흐름까지 이어진다.
 * 인재상을 확보하지 못한 기관은 공통 역량으로 만들고, 그 사실을 응답에 드러낸다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionGenerationService {

    private static final List<String> COMMON_COMPETENCIES = List.of(
            "COMMUNICATION", "PROBLEM_SOLVING", "INTERPERSONAL",
            "ORGANIZATIONAL_UNDERSTANDING", "WORK_ETHIC");

    private final QuestionGenerator questionGenerator;
    private final CompanyRepository companyRepository;
    private final CompetencyEvidenceRepository competencyEvidenceRepository;
    private final CompetencyLibraryRepository competencyLibraryRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    public QuestionGenerateResponse generate(QuestionGenerateRequest request) {
        Company company = companyRepository.findByNameOrAlias(request.companyName().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "등록되지 않은 기관입니다: " + request.companyName()));

        List<CompetencyEvidence> evidences = competencyEvidenceRepository.findByCompanyId(company.getId());
        Map<String, String> labelByCanonicalId = new LinkedHashMap<>();
        List<QuestionContext.Competency> competencies = new ArrayList<>();

        if (evidences.isEmpty()) {
            for (String id : COMMON_COMPETENCIES) {
                CompetencyLibrary c = competencyLibraryRepository.findById(id).orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "공통 역량 시드가 없습니다: " + id));
                labelByCanonicalId.put(c.getCanonicalId(), c.getLabel());
                competencies.add(new QuestionContext.Competency(
                        c.getCanonicalId(), c.getLabel(), c.getLabel(), c.getDefinition()));
            }
        } else {
            for (CompetencyEvidence e : evidences) {
                CompetencyLibrary c = e.getCompetency();
                labelByCanonicalId.put(c.getCanonicalId(), e.getCompanyLabel());
                competencies.add(new QuestionContext.Competency(
                        c.getCanonicalId(), e.getCompanyLabel(), c.getLabel(),
                        e.getDescription() == null || e.getDescription().isBlank()
                                ? c.getDefinition() : e.getDescription()));
            }
        }

        List<GeneratedQuestion> generated = questionGenerator.generate(new QuestionContext(
                company.getName(), company.isCompetencyConfirmed(), request.jobTitle(),
                request.countOrDefault(), competencies));

        validate(generated, labelByCanonicalId.keySet());

        List<GeneratedQuestionResponse> questions = new ArrayList<>();
        for (GeneratedQuestion g : generated) {
            Question saved = questionRepository.saveAndFlush(Question.builder()
                    .company(company)
                    .category(g.category())
                    .questionText(g.questionText())
                    .reusableTemplate(false)
                    .build());

            questions.add(new GeneratedQuestionResponse(
                    saved.getId(), g.questionText(), g.category(), g.intent(),
                    g.targetCanonicalIds().stream()
                            .filter(labelByCanonicalId::containsKey)
                            .map(id -> new GeneratedQuestionResponse.CompetencyTarget(
                                    id, labelByCanonicalId.get(id)))
                            .toList()));
        }

        CompetencyEvidence source = evidences.isEmpty() ? null : evidences.get(0);
        return new QuestionGenerateResponse(
                company.getName(),
                company.isCompetencyConfirmed(),
                source == null ? null : source.getSourceUrl(),
                source == null ? null : source.getSourceTitle(),
                questionGenerator.name(),
                questions);
    }

    /** 생성기가 빈 질문이나 엉뚱한 역량을 내면 저장까지 가지 않는다. */
    private void validate(List<GeneratedQuestion> generated, java.util.Set<String> knownIds) {
        if (generated.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "질문이 생성되지 않았습니다");
        }
        for (GeneratedQuestion g : generated) {
            if (g.questionText() == null || g.questionText().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "빈 질문이 포함돼 있습니다");
            }
            if (g.targetCanonicalIds().stream().noneMatch(knownIds::contains)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "이 기관 역량과 연결되지 않은 질문이 있습니다: " + g.questionText());
            }
        }
    }
}
