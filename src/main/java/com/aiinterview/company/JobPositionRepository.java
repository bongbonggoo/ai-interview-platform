package com.aiinterview.company;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobPositionRepository extends JpaRepository<JobPosition, UUID> {
    List<JobPosition> findByCompanyId(UUID companyId);
}
