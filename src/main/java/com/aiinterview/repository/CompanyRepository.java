package com.aiinterview.repository;

import com.aiinterview.entity.Company;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    Optional<Company> findByName(String name);

    /** 정식명 또는 별칭("코레일", "한전")으로 찾는다. */
    @org.springframework.data.jpa.repository.Query(
            "select distinct c from Company c left join c.aliases a where c.name = :term or a = :term")
    Optional<Company> findByNameOrAlias(@org.springframework.data.repository.query.Param("term") String term);
}
