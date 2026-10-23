package com.collabnexus.deliverables;

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

import java.time.Instant;

@Entity
@Table(name = "deliverable_submissions",
        uniqueConstraints = @UniqueConstraint(name = "uk_deliverable_submission_version",
                columnNames = {"deliverable_id", "version_number"}),
        indexes = @Index(name = "idx_submission_deliverable", columnList = "deliverable_id"))
public class DeliverableSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deliverable_id", nullable = false)
    private Deliverable deliverable;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "submission_url", length = 500)
    private String submissionUrl;

    @Column(length = 2000)
    private String notes;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "stored_file_name", length = 120)
    private String storedFileName;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected DeliverableSubmission() {
    }

    public DeliverableSubmission(Deliverable deliverable, int versionNumber, User submittedBy) {
        this.deliverable = deliverable;
        this.versionNumber = versionNumber;
        this.submittedBy = submittedBy;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Deliverable getDeliverable() {
        return deliverable;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getSubmissionUrl() {
        return submissionUrl;
    }

    public void setSubmissionUrl(String submissionUrl) {
        this.submissionUrl = submissionUrl;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public User getSubmittedBy() {
        return submittedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
