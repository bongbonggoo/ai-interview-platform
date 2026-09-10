package com.aiinterview.config;

import com.aiinterview.entity.Company;
import com.aiinterview.entity.CompetencyEvidence;
import com.aiinterview.entity.CompetencyLibrary;
import com.aiinterview.repository.CompanyRepository;
import com.aiinterview.repository.CompetencyEvidenceRepository;
import com.aiinterview.repository.CompetencyLibraryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 시드는 "미리 리서치해둔 스냅샷"이다. 기관 인재상은 출처를 확인한 것만 싣고,
 * 확인하지 못한 기관은 역량 없이(confirmed=false) 넣어 공통 기준으로만 평가되게 한다.
 * 추측으로 채우면 근거 없는 역량으로 채점하게 되므로 절대 하지 않는다.
 *
 * 멱등하다 — 뜰 때마다 같은 내용으로 덮어써서, 시드 파일을 고치고 재기동하면 반영된다.
 */
@Component
@RequiredArgsConstructor
public class SeedDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataLoader.class);

    private final ObjectMapper objectMapper;
    private final CompanyRepository companyRepository;
    private final CompetencyLibraryRepository competencyLibraryRepository;
    private final CompetencyEvidenceRepository competencyEvidenceRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        int competencies = loadCompetencies();
        int companies = loadCompanies();
        log.info("시드 적재 완료: 역량 {}개, 기관 {}개", competencies, companies);
    }

    private int loadCompetencies() throws Exception {
        JsonNode root = read("seed/competencies.json").path("competencies");
        for (JsonNode n : root) {
            String id = n.path("canonicalId").asText();
            CompetencyLibrary c = competencyLibraryRepository.findById(id)
                    .orElseGet(() -> CompetencyLibrary.builder().canonicalId(id).build());
            c.setLabel(n.path("label").asText());
            c.setDefinition(text(n, "definition"));
            c.setTier(text(n, "tier"));
            c.setAnchor1(text(n, "anchor1"));
            c.setAnchor3(text(n, "anchor3"));
            c.setAnchor5(text(n, "anchor5"));
            competencyLibraryRepository.save(c);
        }
        return root.size();
    }

    private int loadCompanies() throws Exception {
        JsonNode root = read("seed/companies.json").path("companies");
        for (JsonNode n : root) {
            String name = n.path("name").asText();
            Company company = companyRepository.findByName(name)
                    .orElseGet(() -> Company.builder().name(name).build());

            company.setOfficialDomain(text(n, "officialDomain"));
            boolean confirmed = n.path("confirmed").asBoolean(false);
            company.setCompetencyConfirmed(confirmed);
            company.setStatus(confirmed ? "VERIFIED" : "DRAFT_LIBRARY");

            Set<String> aliases = new LinkedHashSet<>();
            n.path("aliases").forEach(a -> aliases.add(a.asText()));
            company.setAliases(aliases);
            companyRepository.save(company);

            // 시드 파일이 유일한 기준이 되도록 기존 근거를 지우고 다시 넣는다.
            competencyEvidenceRepository.deleteByCompanyId(company.getId());
            for (JsonNode c : n.path("competencies")) {
                String canonicalId = c.path("canonicalId").asText();
                CompetencyLibrary competency = competencyLibraryRepository.findById(canonicalId)
                        .orElseThrow(() -> new IllegalStateException(
                                "시드의 기관 역량이 역량 라이브러리에 없습니다: " + canonicalId));
                competencyEvidenceRepository.save(CompetencyEvidence.builder()
                        .company(company)
                        .competency(competency)
                        .companyLabel(c.path("companyLabel").asText())
                        .description(text(c, "description"))
                        .sourceUrl(n.path("sourceUrl").asText())
                        .sourceTitle(text(n, "sourceTitle"))
                        .build());
            }
        }
        return root.size();
    }

    private JsonNode read(String path) throws Exception {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readTree(in);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }
}
