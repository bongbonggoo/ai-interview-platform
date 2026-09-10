package com.aiinterview.controller;

import com.aiinterview.dto.request.CompanyCreateRequest;
import com.aiinterview.dto.response.CompanyResponse;
import com.aiinterview.service.CompanyService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 1차 성공 기준: POST /companies 로 저장 -> GET /companies/{id} 로 조회.
 * 예) POST { "name": "한국철도공사", "officialDomain": "info.korail.com" }
 */
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse create(@Valid @RequestBody CompanyCreateRequest request) {
        return companyService.create(request);
    }

    @GetMapping("/{id}")
    public CompanyResponse getById(@PathVariable UUID id) {
        return companyService.getById(id);
    }

    @GetMapping
    public List<CompanyResponse> getAll() {
        return companyService.getAll();
    }
}
