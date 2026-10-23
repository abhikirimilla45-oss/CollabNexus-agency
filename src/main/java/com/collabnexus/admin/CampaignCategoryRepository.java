package com.collabnexus.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampaignCategoryRepository extends JpaRepository<CampaignCategory, Long> {

    List<CampaignCategory> findAllByOrderBySortOrderAscNameAsc();

    List<CampaignCategory> findByActiveTrueOrderBySortOrderAscNameAsc();

    Optional<CampaignCategory> findBySlugIgnoreCase(String slug);

    boolean existsByNameIgnoreCase(String name);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, Long id);
}
