package com.collabnexus.contracts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractVersionRepository extends JpaRepository<ContractVersion, Long> {

    List<ContractVersion> findByContractIdOrderByVersionNumberDesc(Long contractId);
}
