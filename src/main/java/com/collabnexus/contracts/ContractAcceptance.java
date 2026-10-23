package com.collabnexus.contracts;

import com.collabnexus.users.Role;
import com.collabnexus.users.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "contract_acceptances",
        uniqueConstraints = @UniqueConstraint(name = "uk_contract_acceptance",
                columnNames = {"contract_id", "version_number", "accepted_by"}),
        indexes = @Index(name = "idx_contract_acceptance_contract", columnList = "contract_id"))
public class ContractAcceptance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accepted_by", nullable = false)
    private User acceptedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "accepted_at", nullable = false, updatable = false)
    private Instant acceptedAt;

    protected ContractAcceptance() {
    }

    public ContractAcceptance(Contract contract, int versionNumber, User acceptedBy, Role role) {
        this.contract = contract;
        this.versionNumber = versionNumber;
        this.acceptedBy = acceptedBy;
        this.role = role;
    }

    @PrePersist
    void onCreate() {
        this.acceptedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Contract getContract() {
        return contract;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public User getAcceptedBy() {
        return acceptedBy;
    }

    public Role getRole() {
        return role;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }
}
