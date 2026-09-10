package com.aiinterview.rubric;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RubricVersionRepository extends JpaRepository<RubricVersion, UUID> {
    List<RubricVersion> findByCompanyIdOrderByVersionNumberDesc(UUID companyId);
    Optional<RubricVersion> findByCompanyIdAndVersionNumber(UUID companyId, Integer versionNumber);
}
