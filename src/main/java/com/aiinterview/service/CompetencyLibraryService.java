package com.aiinterview.service;

import com.aiinterview.dto.request.CompetencyUpsertRequest;
import com.aiinterview.dto.response.CompetencyResponse;
import com.aiinterview.entity.CompetencyLibrary;
import com.aiinterview.entity.EvaluationCriterion;
import com.aiinterview.repository.CompetencyLibraryRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 역량 라이브러리는 회사 무관 재사용 자산이다(CLAUDE.md 원칙 2).
 * 그래서 여기는 "회사별 역량 만들기"용 API가 아니라 canonical 자산을 관리/조회하는 관리자용 창구다.
 * 루브릭에서 매칭 안 되는 이름이 나오면 새로 만들지 말고 CompetencyMatchCandidate(2차)로 쌓아야 한다.
 */
@Service
@RequiredArgsConstructor
public class CompetencyLibraryService {

    private final CompetencyLibraryRepository competencyLibraryRepository;

    /** canonical_id는 사람이 읽는 안정 식별자이므로 PUT(멱등 upsert)으로 다룬다. */
    @Transactional
    public CompetencyResponse upsert(String canonicalId, CompetencyUpsertRequest request) {
        CompetencyLibrary competency = competencyLibraryRepository.findById(canonicalId)
                .orElseGet(() -> CompetencyLibrary.builder().canonicalId(canonicalId).build());

        competency.setLabel(request.label());
        competency.setDefinition(request.definition());
        competency.setTier(request.tier());

        return CompetencyResponse.from(competencyLibraryRepository.save(competency));
    }

    public List<CompetencyResponse> getAll() {
        return competencyLibraryRepository.findAll().stream()
                .map(CompetencyResponse::from)
                .toList();
    }

    public CompetencyResponse getById(String canonicalId) {
        return CompetencyResponse.from(getEntity(canonicalId));
    }

    /**
     * 루브릭 기준(EvaluationCriterion)이 참조할 때 쓰는 조회.
     * 없으면 404 — 여기서 자동 생성하지 않는 것이 원칙 2를 코드로 지키는 지점이다.
     */
    public CompetencyLibrary getEntity(String canonicalId) {
        return competencyLibraryRepository.findById(canonicalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "역량 라이브러리에 없는 canonical_id입니다: " + canonicalId
                                + " (회사별로 새 역량을 만들지 말고 기존 canonical 역량에 매칭할 것)"));
    }
}
