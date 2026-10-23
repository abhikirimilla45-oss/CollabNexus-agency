package com.collabnexus.payments;

import com.collabnexus.brands.BrandProfile;
import com.collabnexus.brands.BrandProfileRepository;
import com.collabnexus.collaborations.Collaboration;
import com.collabnexus.collaborations.CollaborationRepository;
import com.collabnexus.collaborations.CollaborationStatus;
import com.collabnexus.common.Exceptions;
import com.collabnexus.common.PageView;
import com.collabnexus.creators.CreatorProfile;
import com.collabnexus.creators.CreatorProfileRepository;
import com.collabnexus.notifications.NotificationService;
import com.collabnexus.notifications.NotificationType;
import com.collabnexus.users.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class PaymentService {

    private static final Map<PaymentStatus, Set<PaymentStatus>> TRANSITIONS = transitions();

    private final PaymentRepository paymentRepository;
    private final CollaborationRepository collaborationRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final NotificationService notificationService;

    public PaymentService(PaymentRepository paymentRepository,
                          CollaborationRepository collaborationRepository,
                          BrandProfileRepository brandProfileRepository,
                          CreatorProfileRepository creatorProfileRepository,
                          NotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.collaborationRepository = collaborationRepository;
        this.brandProfileRepository = brandProfileRepository;
        this.creatorProfileRepository = creatorProfileRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public PaymentView create(Long userId, Long collaborationId, PaymentRequest request) {
        BrandProfile brand = brandProfileRepository.findByUserId(userId)
                .orElseThrow(() -> Exceptions.notFound("Brand profile not found."));
        Collaboration collaboration = collaborationRepository.findByIdAndBrandUserId(collaborationId, userId)
                .orElseThrow(() -> Exceptions.notFound("Collaboration not found."));
        if (collaboration.getStatus() == CollaborationStatus.CANCELLED) {
            throw Exceptions.conflict("COLLABORATION_CLOSED",
                    "Payment milestones cannot be added to a cancelled collaboration.");
        }

        BigDecimal amount = request.amount().setScale(2, java.math.RoundingMode.HALF_UP);
        if (amount.signum() <= 0) {
            throw Exceptions.badRequest("INVALID_AMOUNT", "The payment amount must be greater than zero.");
        }
        BigDecimal alreadyScheduled = paymentRepository.sumAmountByCollaborationId(collaborationId);
        BigDecimal agreed = collaboration.getAgreedAmount();
        if (agreed != null && agreed.signum() > 0
                && alreadyScheduled.add(amount).compareTo(agreed) > 0) {
            throw Exceptions.conflict("PAYMENT_EXCEEDS_AGREED_AMOUNT",
                    "These milestones would exceed the agreed collaboration amount.");
        }

        String currency = StringUtils.hasText(request.currency())
                ? request.currency().trim().toUpperCase(Locale.ROOT)
                : collaboration.getCurrency();
        PaymentRecord payment = new PaymentRecord(collaboration, request.milestone().trim(), amount, currency);
        payment.setDueDate(request.dueDate());
        payment.setNotes(trimToNull(request.notes()));
        payment.setStatus(request.dueDate() != null && !request.dueDate().isAfter(LocalDate.now())
                ? PaymentStatus.DUE : PaymentStatus.NOT_DUE);
        payment = paymentRepository.saveAndFlush(payment);

        notificationService.notify(collaboration.getCreator().getUser(), NotificationType.PAYMENT_UPDATED,
                "New payment milestone recorded",
                brand.getCompanyName() + " added the milestone \"" + payment.getMilestone() + "\".",
                "/dashboard.html");

        return PaymentView.from(payment);
    }

    @Transactional(readOnly = true)
    public PageView<PaymentView> list(Long userId, Role role, PaymentStatus status, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<PaymentRecord> results;
        if (role == Role.BRAND) {
            BrandProfile brand = brandProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> Exceptions.notFound("Brand profile not found."));
            results = status == null
                    ? paymentRepository.findByBrandId(brand.getId(), pageRequest)
                    : paymentRepository.findByBrandIdAndStatus(brand.getId(), status, pageRequest);
        } else if (role == Role.CREATOR) {
            CreatorProfile creator = creatorProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> Exceptions.notFound("Creator profile not found."));
            results = status == null
                    ? paymentRepository.findByCreatorId(creator.getId(), pageRequest)
                    : paymentRepository.findByCreatorIdAndStatus(creator.getId(), status, pageRequest);
        } else {
            results = status == null
                    ? paymentRepository.findAll(pageRequest)
                    : paymentRepository.findByStatus(status, pageRequest);
        }
        return PageView.of(results, PaymentView::from);
    }

    @Transactional(readOnly = true)
    public PaymentView detail(Long userId, Role role, Long paymentId) {
        return PaymentView.from(readable(userId, role, paymentId));
    }

    @Transactional(readOnly = true)
    public PaymentSummary summary(Long userId, Role role) {
        BigDecimal scheduled;
        BigDecimal paid;
        long count;
        Map<String, Long> byStatus = new LinkedHashMap<>();

        if (role == Role.BRAND) {
            BrandProfile brand = brandProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> Exceptions.notFound("Brand profile not found."));
            scheduled = paymentRepository.sumScheduledByBrandId(brand.getId());
            paid = paymentRepository.sumPaidByBrandId(brand.getId());
            count = paymentRepository.countByBrandId(brand.getId());
            for (PaymentStatus status : PaymentStatus.values()) {
                byStatus.put(status.name(), paymentRepository.countByBrandIdAndStatus(brand.getId(), status));
            }
        } else if (role == Role.CREATOR) {
            CreatorProfile creator = creatorProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> Exceptions.notFound("Creator profile not found."));
            scheduled = paymentRepository.sumScheduledByCreatorId(creator.getId());
            paid = paymentRepository.sumPaidByCreatorId(creator.getId());
            count = paymentRepository.countByCreatorId(creator.getId());
            for (PaymentStatus status : PaymentStatus.values()) {
                byStatus.put(status.name(), paymentRepository.countByCreatorIdAndStatus(creator.getId(), status));
            }
        } else {
            scheduled = paymentRepository.sumAllScheduled();
            paid = paymentRepository.sumAllPaid();
            count = paymentRepository.count();
            for (PaymentStatus status : PaymentStatus.values()) {
                byStatus.put(status.name(), paymentRepository.countByStatus(status));
            }
        }

        long paidCount = byStatus.getOrDefault(PaymentStatus.PAID.name(), 0L);
        return new PaymentSummary(scheduled, paid, scheduled.subtract(paid).max(BigDecimal.ZERO),
                count, paidCount, count - paidCount, "USD", byStatus);
    }

    @Transactional
    public PaymentView updateStatus(Long userId, Role role, Long paymentId, PaymentStatusRequest request) {
        PaymentRecord payment = readable(userId, role, paymentId);
        if (role == Role.CREATOR && request.status() != PaymentStatus.DISPUTED) {
            throw Exceptions.forbidden("Creators can only mark a payment record as disputed.");
        }
        if (role == Role.BRAND) {
            brandProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> Exceptions.notFound("Brand profile not found."));
        }

        if (payment.getStatus() == request.status()) {
            return PaymentView.from(payment);
        }
        Set<PaymentStatus> allowed = TRANSITIONS.getOrDefault(payment.getStatus(), EnumSet.noneOf(PaymentStatus.class));
        if (!allowed.contains(request.status())) {
            throw Exceptions.conflict("INVALID_PAYMENT_TRANSITION",
                    "A payment record cannot move from " + payment.getStatus() + " to " + request.status() + ".");
        }

        applyStatus(payment, request.status(), request);
        payment = paymentRepository.saveAndFlush(payment);
        notifyCounterpart(payment, request.status());

        return PaymentView.from(payment);
    }

    private void applyStatus(PaymentRecord payment, PaymentStatus target, PaymentStatusRequest request) {
        if (target == PaymentStatus.PAID) {
            payment.setPaidAmount(payment.getAmount());
            payment.setPaidAt(Instant.now());
        } else if (target == PaymentStatus.REFUNDED) {
            payment.setPaidAmount(BigDecimal.ZERO);
        } else if (request.paidAmount() != null) {
            BigDecimal paid = request.paidAmount().setScale(2, java.math.RoundingMode.HALF_UP);
            if (paid.signum() < 0 || paid.compareTo(payment.getAmount()) > 0) {
                throw Exceptions.badRequest("INVALID_PAID_AMOUNT",
                        "The paid amount must be between zero and the scheduled amount.");
            }
            payment.setPaidAmount(paid);
        }
        payment.setStatus(target);
        payment.setRecordedAt(Instant.now());
        if (StringUtils.hasText(request.externalReference())) {
            payment.setExternalReference(request.externalReference().trim());
        }
        if (StringUtils.hasText(request.notes())) {
            payment.setNotes(request.notes().trim());
        }
    }

    private void notifyCounterpart(PaymentRecord payment, PaymentStatus status) {
        Long brandUserId = payment.getBrand().getUser().getId();
        Long creatorUserId = payment.getCreator().getUser().getId();
        String message = "Payment milestone \"" + payment.getMilestone() + "\" is now "
                + status.name().replace('_', ' ') + ". This is a recorded status, not a verified transfer.";
        notificationService.notify(brandUserId, NotificationType.PAYMENT_UPDATED,
                "Payment status updated", message, "/dashboard.html");
        notificationService.notify(creatorUserId, NotificationType.PAYMENT_UPDATED,
                "Payment status updated", message, "/dashboard.html");
    }

    private PaymentRecord readable(Long userId, Role role, Long paymentId) {
        if (role == Role.BRAND) {
            return paymentRepository.findByIdAndCollaborationBrandUserId(paymentId, userId)
                    .orElseThrow(() -> Exceptions.notFound("Payment record not found."));
        }
        if (role == Role.CREATOR) {
            return paymentRepository.findByIdAndCollaborationCreatorUserId(paymentId, userId)
                    .orElseThrow(() -> Exceptions.notFound("Payment record not found."));
        }
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> Exceptions.notFound("Payment record not found."));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static Map<PaymentStatus, Set<PaymentStatus>> transitions() {
        Map<PaymentStatus, Set<PaymentStatus>> map = new EnumMap<>(PaymentStatus.class);
        map.put(PaymentStatus.NOT_DUE, EnumSet.of(PaymentStatus.DUE, PaymentStatus.PROCESSING,
                PaymentStatus.PAID, PaymentStatus.FAILED, PaymentStatus.DISPUTED));
        map.put(PaymentStatus.DUE, EnumSet.of(PaymentStatus.PROCESSING, PaymentStatus.PAID,
                PaymentStatus.FAILED, PaymentStatus.DISPUTED));
        map.put(PaymentStatus.PROCESSING, EnumSet.of(PaymentStatus.PAID, PaymentStatus.FAILED,
                PaymentStatus.DISPUTED));
        map.put(PaymentStatus.PAID, EnumSet.of(PaymentStatus.REFUNDED, PaymentStatus.DISPUTED));
        map.put(PaymentStatus.FAILED, EnumSet.of(PaymentStatus.DUE, PaymentStatus.PROCESSING,
                PaymentStatus.DISPUTED));
        map.put(PaymentStatus.REFUNDED, EnumSet.noneOf(PaymentStatus.class));
        map.put(PaymentStatus.DISPUTED, EnumSet.of(PaymentStatus.DUE, PaymentStatus.PROCESSING,
                PaymentStatus.PAID, PaymentStatus.FAILED, PaymentStatus.REFUNDED));
        return map;
    }
}
