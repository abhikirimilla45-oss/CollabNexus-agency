package com.collabnexus.contracts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractAcceptanceRepository extends JpaRepository<ContractAcceptance, Long> {

    List<ContractAcceptance> findByContractIdOrderByAcceptedAtDesc(Long contractId);

    boolean existsByContractIdAndVersionNumberAndAcceptedById(Long contractId, int versionNumber,
                                                              Long userId);
}
