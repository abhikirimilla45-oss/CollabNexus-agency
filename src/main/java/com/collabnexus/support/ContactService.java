package com.collabnexus.support;

import com.collabnexus.common.Exceptions;
import com.collabnexus.common.PageView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class ContactService {

    private final ContactMessageRepository contactMessageRepository;

    public ContactService(ContactMessageRepository contactMessageRepository) {
        this.contactMessageRepository = contactMessageRepository;
    }

    @Transactional
    public ContactReceipt submit(ContactRequest request) {
        ContactMessage message = new ContactMessage(
                request.name().trim(),
                request.email().trim().toLowerCase(Locale.ROOT),
                request.role(),
                request.topic(),
                request.message().trim());
        contactMessageRepository.save(message);
        return new ContactReceipt(message.getId(), message.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public PageView<ContactMessageView> list(ContactStatus status, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize);
        Page<ContactMessage> results = status == null
                ? contactMessageRepository.findAllByOrderByCreatedAtDesc(pageRequest)
                : contactMessageRepository.findByStatusOrderByCreatedAtDesc(status, pageRequest);
        return PageView.of(results, ContactMessageView::from);
    }

    @Transactional
    public ContactMessageView handle(Long id, Long adminId) {
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> Exceptions.notFound("Contact message not found."));
        message.markHandled(adminId);
        contactMessageRepository.save(message);
        return ContactMessageView.from(message);
    }

    public record ContactReceipt(Long id, Instant receivedAt) {
    }
}
