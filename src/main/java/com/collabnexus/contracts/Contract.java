package com.collabnexus.contracts;

import com.collabnexus.brands.BrandProfile;
import com.collabnexus.collaborations.Collaboration;
import com.collabnexus.creators.CreatorProfile;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "contracts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_contract_collaboration", columnNames = "collaboration_id"),
                @UniqueConstraint(name = "uk_contract_reference", columnNames = "reference_number")
        },
        indexes = {
                @Index(name = "idx_contract_brand", columnList = "brand_id"),
                @Index(name = "idx_contract_creator", columnList = "creator_id")
        })
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collaboration_id", nullable = false, unique = true)
    private Collaboration collaboration;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private BrandProfile brand;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private CreatorProfile creator;

    @Column(name = "reference_number", nullable = false, length = 40)
    private String referenceNumber;

    @Column(name = "agreed_compensation", precision = 12, scale = 2)
    private BigDecimal agreedCompensation;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "deliverables_summary", length = 4000)
    private String deliverablesSummary;

    @Column(name = "completion_deadline")
    private LocalDate completionDeadline;

    @Column(name = "usage_rights", length = 4000)
    private String usageRights;

    @Column(name = "revision_allowance", nullable = false)
    private int revisionAllowance = 1;

    @Column(name = "cancellation_terms", length = 4000)
    private String cancellationTerms;

    @Column(name = "payment_schedule", length = 4000)
    private String paymentSchedule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContractStatus status = ContractStatus.DRAFT;

    @Column(name = "version_number", nullable = false)
    private int versionNumber = 1;

    @Column(name = "brand_acknowledged_at")
    private Instant brandAcknowledgedAt;

    @Column(name = "creator_acknowledged_at")
    private Instant creatorAcknowledgedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Contract() {
    }

    public Contract(Collaboration collaboration, String referenceNumber) {
        this.collaboration = collaboration;
        this.brand = collaboration.getBrand();
        this.creator = collaboration.getCreator();
        this.referenceNumber = referenceNumber;
        this.agreedCompensation = collaboration.getAgreedAmount();
        this.currency = collaboration.getCurrency();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Collaboration getCollaboration() {
        return collaboration;
    }

    public BrandProfile getBrand() {
        return brand;
    }

    public CreatorProfile getCreator() {
        return creator;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public BigDecimal getAgreedCompensation() {
        return agreedCompensation;
    }

    public void setAgreedCompensation(BigDecimal agreedCompensation) {
        this.agreedCompensation = agreedCompensation;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDeliverablesSummary() {
        return deliverablesSummary;
    }

    public void setDeliverablesSummary(String deliverablesSummary) {
        this.deliverablesSummary = deliverablesSummary;
    }

    public LocalDate getCompletionDeadline() {
        return completionDeadline;
    }

    public void setCompletionDeadline(LocalDate completionDeadline) {
        this.completionDeadline = completionDeadline;
    }

    public String getUsageRights() {
        return usageRights;
    }

    public void setUsageRights(String usageRights) {
        this.usageRights = usageRights;
    }

    public int getRevisionAllowance() {
        return revisionAllowance;
    }

    public void setRevisionAllowance(int revisionAllowance) {
        this.revisionAllowance = revisionAllowance;
    }

    public String getCancellationTerms() {
        return cancellationTerms;
    }

    public void setCancellationTerms(String cancellationTerms) {
        this.cancellationTerms = cancellationTerms;
    }

    public String getPaymentSchedule() {
        return paymentSchedule;
    }

    public void setPaymentSchedule(String paymentSchedule) {
        this.paymentSchedule = paymentSchedule;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public void setStatus(ContractStatus status) {
        this.status = status;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public Instant getBrandAcknowledgedAt() {
        return brandAcknowledgedAt;
    }

    public void setBrandAcknowledgedAt(Instant brandAcknowledgedAt) {
        this.brandAcknowledgedAt = brandAcknowledgedAt;
    }

    public Instant getCreatorAcknowledgedAt() {
        return creatorAcknowledgedAt;
    }

    public void setCreatorAcknowledgedAt(Instant creatorAcknowledgedAt) {
        this.creatorAcknowledgedAt = creatorAcknowledgedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
