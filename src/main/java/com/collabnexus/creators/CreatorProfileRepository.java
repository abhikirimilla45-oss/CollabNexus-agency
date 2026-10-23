package com.collabnexus.creators;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CreatorProfileRepository extends JpaRepository<CreatorProfile, Long>,
        JpaSpecificationExecutor<CreatorProfile> {

    Optional<CreatorProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
