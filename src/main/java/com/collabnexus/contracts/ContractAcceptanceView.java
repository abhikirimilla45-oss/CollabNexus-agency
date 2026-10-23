package com.collabnexus.contracts;

import com.collabnexus.users.Role;

import java.time.Instant;

public record ContractAcceptanceView(
        Long id,
        int versionNumber,
        String acceptedByName,
        Role role,
        Instant acceptedAt
) {

    public static ContractAcceptanceView from(ContractAcceptance acceptance) {
        return new ContractAcceptanceView(
                acceptance.getId(),
                acceptance.getVersionNumber(),
                acceptance.getAcceptedBy().getFullName(),
                acceptance.getRole(),
                acceptance.getAcceptedAt()
        );
    }
}
