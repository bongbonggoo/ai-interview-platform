package com.aiinterview.company;

import com.aiinterview.company.dto.CompanyCreateRequest;
import com.aiinterview.company.dto.CompanyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    @Transactional
    public CompanyResponse create(CompanyCreateRequest request) {
        companyRepository.findByName(request.name()).ifPresent(c -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 회사명입니다: " + request.name());
        });

        Company company = Company.builder()
                .name(request.name())
                .officialDomain(request.officialDomain())
                .status("draft_library")
                .build();

        return CompanyResponse.from(companyRepository.saveAndFlush(company));
    }

    public CompanyResponse getById(UUID id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회사를 찾을 수 없습니다: " + id));
        return CompanyResponse.from(company);
    }

    public List<CompanyResponse> getAll() {
        return companyRepository.findAll().stream()
                .map(CompanyResponse::from)
                .toList();
    }
}
