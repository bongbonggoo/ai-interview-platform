package com.aiinterview.repository;

import com.aiinterview.entity.CompetencyEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CompetencyEvidenceRepository extends JpaRepository<CompetencyEvidence, UUID> {
    List<CompetencyEvidence> findByCompanyId(UUID companyId);
    void deleteByCompanyId(UUID companyId);
}
