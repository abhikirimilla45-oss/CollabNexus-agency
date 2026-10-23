package com.collabnexus.contracts;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    Optional<Contract> findByCollaborationId(Long collaborationId);

    Optional<Contract> findByIdAndCollaborationBrandUserId(Long id, Long userId);

    Optional<Contract> findByIdAndCollaborationCreatorUserId(Long id, Long userId);

    Page<Contract> findByBrandUserId(Long userId, Pageable pageable);

    Page<Contract> findByCreatorUserId(Long userId, Pageable pageable);
}
