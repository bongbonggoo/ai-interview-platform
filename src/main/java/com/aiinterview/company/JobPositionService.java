package com.aiinterview.company;

import com.aiinterview.company.dto.JobPositionCreateRequest;
import com.aiinterview.company.dto.JobPositionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** InterviewSession.jobPosition이 필수라, 세션을 만들려면 직무가 먼저 있어야 한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobPositionService {

    private final JobPositionRepository jobPositionRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public JobPositionResponse create(UUID companyId, JobPositionCreateRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회사를 찾을 수 없습니다: " + companyId));

        return JobPositionResponse.from(jobPositionRepository.saveAndFlush(
                JobPosition.builder().company(company).name(request.name()).build()));
    }

    public List<JobPositionResponse> getByCompany(UUID companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회사를 찾을 수 없습니다: " + companyId);
        }
        return jobPositionRepository.findByCompanyId(companyId).stream()
                .map(JobPositionResponse::from)
                .toList();
    }

    public JobPosition getEntity(UUID id) {
        return jobPositionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "직무를 찾을 수 없습니다: " + id));
    }
}
