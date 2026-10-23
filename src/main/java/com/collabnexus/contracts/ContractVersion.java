package com.collabnexus.contracts;

import com.collabnexus.users.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "contract_versions",
        uniqueConstraints = @UniqueConstraint(name = "uk_contract_version",
                columnNames = {"contract_id", "version_number"}),
        indexes = @Index(name = "idx_contract_version_contract", columnList = "contract_id"))
public class ContractVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "agreed_compensation", precision = 12, scale = 2)
    private BigDecimal agreedCompensation;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "deliverables_summary", length = 4000)
    private String deliverablesSummary;

    @Column(name = "completion_deadline")
    private LocalDate completionDeadline;

    @Column(name = "usage_rights", length = 4000)
    private String usageRights;

    @Column(name = "revision_allowance", nullable = false)
    private int revisionAllowance;

    @Column(name = "cancellation_terms", length = 4000)
    private String cancellationTerms;

    @Column(name = "payment_schedule", length = 4000)
    private String paymentSchedule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ContractVersion() {
    }

    public ContractVersion(Contract contract, User createdBy) {
        this.contract = contract;
        this.versionNumber = contract.getVersionNumber();
        this.agreedCompensation = contract.getAgreedCompensation();
        this.currency = contract.getCurrency();
        this.deliverablesSummary = contract.getDeliverablesSummary();
        this.completionDeadline = contract.getCompletionDeadline();
        this.usageRights = contract.getUsageRights();
        this.revisionAllowance = contract.getRevisionAllowance();
        this.cancellationTerms = contract.getCancellationTerms();
        this.paymentSchedule = contract.getPaymentSchedule();
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
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

    public BigDecimal getAgreedCompensation() {
        return agreedCompensation;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDeliverablesSummary() {
        return deliverablesSummary;
    }

    public LocalDate getCompletionDeadline() {
        return completionDeadline;
    }

    public String getUsageRights() {
        return usageRights;
    }

    public int getRevisionAllowance() {
        return revisionAllowance;
    }

    public String getCancellationTerms() {
        return cancellationTerms;
    }

    public String getPaymentSchedule() {
        return paymentSchedule;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
