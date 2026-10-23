package com.collabnexus.support;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A contact request submitted from the public contact page. Messages are only
 * visible to platform administrators and are never exposed through public APIs.
 */
@Entity
@Table(name = "contact_messages", indexes = {
        @Index(name = "idx_contact_created", columnList = "created_at"),
        @Index(name = "idx_contact_status", columnList = "status,created_at")
})
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 180)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContactRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContactTopic topic;

    @Column(nullable = false, length = 4000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContactStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "handled_at")
    private Instant handledAt;

    @Column(name = "handled_by")
    private Long handledBy;

    protected ContactMessage() {
    }

    public ContactMessage(String name, String email, ContactRole role, ContactTopic topic, String message) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.topic = topic;
        this.message = message;
        this.status = ContactStatus.NEW;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public ContactRole getRole() {
        return role;
    }

    public ContactTopic getTopic() {
        return topic;
    }

    public String getMessage() {
        return message;
    }

    public ContactStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getHandledAt() {
        return handledAt;
    }

    public Long getHandledBy() {
        return handledBy;
    }

    public void markHandled(Long adminId) {
        if (this.status == ContactStatus.HANDLED) {
            return;
        }
        this.status = ContactStatus.HANDLED;
        this.handledAt = Instant.now();
        this.handledBy = adminId;
    }
}
