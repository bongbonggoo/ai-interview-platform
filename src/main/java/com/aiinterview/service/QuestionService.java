package com.aiinterview.service;

import com.aiinterview.dto.request.QuestionCreateRequest;
import com.aiinterview.dto.response.QuestionResponse;
import com.aiinterview.entity.Company;
import com.aiinterview.entity.Question;
import com.aiinterview.repository.CompanyRepository;
import com.aiinterview.repository.QuestionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Question은 지금 Company 종속이다(CLAUDE.md 원칙 5). 그래서 생성 API도 회사 하위 경로로만 열고,
 * 회사 없이 떠다니는 질문은 만들 수 없게 한다. QuestionTemplate 분리는 의도적으로 나중 작업.
 */
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public QuestionResponse create(UUID companyId, QuestionCreateRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회사를 찾을 수 없습니다: " + companyId));

        Question question = Question.builder()
                .company(company)
                .category(request.category())
                .questionText(request.questionText())
                .reusableTemplate(Boolean.TRUE.equals(request.reusableTemplate()))
                .build();

        return QuestionResponse.from(questionRepository.saveAndFlush(question));
    }

    public List<QuestionResponse> getByCompany(UUID companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회사를 찾을 수 없습니다: " + companyId);
        }
        return questionRepository.findByCompanyId(companyId).stream()
                .map(QuestionResponse::from)
                .toList();
    }

    public QuestionResponse getById(UUID id) {
        return QuestionResponse.from(getEntity(id));
    }

    /** 다른 서비스(루브릭 등)가 FK로 붙일 때 쓰는 엔티티 조회. */
    public Question getEntity(UUID id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다: " + id));
    }
}
