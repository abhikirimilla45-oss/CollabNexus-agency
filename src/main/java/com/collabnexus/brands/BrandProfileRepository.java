package com.collabnexus.brands;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BrandProfileRepository extends JpaRepository<BrandProfile, Long> {

    Optional<BrandProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
