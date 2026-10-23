package com.collabnexus.contracts;

import com.collabnexus.brands.BrandProfileRepository;
import com.collabnexus.collaborations.Collaboration;
import com.collabnexus.collaborations.CollaborationRepository;
import com.collabnexus.collaborations.CollaborationStatus;
import com.collabnexus.common.Exceptions;
import com.collabnexus.common.PageView;
import com.collabnexus.creators.CreatorProfileRepository;
import com.collabnexus.notifications.NotificationService;
import com.collabnexus.notifications.NotificationType;
import com.collabnexus.users.Role;
import com.collabnexus.users.User;
import com.collabnexus.users.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
public class ContractService {

    private static final String DEFAULT_USAGE_RIGHTS =
            "Usage rights and licensing must be agreed in the campaign brief before both parties "
                    + "acknowledge this record.";
    private static final String DEFAULT_CANCELLATION_TERMS =
            "Either party may request cancellation in writing. Work completed before cancellation "
                    + "remains subject to the recorded payment terms.";
    private static final String DEFAULT_PAYMENT_SCHEDULE =
            "Manual milestone tracking only. CollabNexus records status changes and does not "
                    + "transfer money or verify external payment.";

    private final ContractRepository contractRepository;
    private final ContractVersionRepository versionRepository;
    private final ContractAcceptanceRepository acceptanceRepository;
    private final CollaborationRepository collaborationRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ContractService(ContractRepository contractRepository,
                           ContractVersionRepository versionRepository,
                           ContractAcceptanceRepository acceptanceRepository,
                           CollaborationRepository collaborationRepository,
                           BrandProfileRepository brandProfileRepository,
                           CreatorProfileRepository creatorProfileRepository,
                           UserRepository userRepository,
                           NotificationService notificationService) {
        this.contractRepository = contractRepository;
        this.versionRepository = versionRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.collaborationRepository = collaborationRepository;
        this.brandProfileRepository = brandProfileRepository;
        this.creatorProfileRepository = creatorProfileRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Contract createForCollaboration(Collaboration collaboration) {
        return contractRepository.findByCollaborationId(collaboration.getId())
                .orElseGet(() -> create(collaboration));
    }

    @Transactional(readOnly = true)
    public PageView<ContractView> list(Long userId, Role role, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "updatedAt"));

        Page<Contract> results;
        if (role == Role.BRAND) {
            brandProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> Exceptions.notFound("Brand profile not found."));
            results = contractRepository.findByBrandUserId(userId, pageRequest);
        } else if (role == Role.CREATOR) {
            creatorProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> Exceptions.notFound("Creator profile not found."));
            results = contractRepository.findByCreatorUserId(userId, pageRequest);
        } else {
            results = contractRepository.findAll(pageRequest);
        }
        return PageView.of(results, this::toView);
    }

    @Transactional(readOnly = true)
    public ContractView detail(Long userId, Role role, Long contractId) {
        return toView(readable(userId, role, contractId));
    }

    @Transactional(readOnly = true)
    public ContractView forCollaboration(Long userId, Role role, Long collaborationId) {
        readableCollaboration(userId, role, collaborationId);
        Contract contract = contractRepository.findByCollaborationId(collaborationId)
                .orElseThrow(() -> Exceptions.notFound("Contract not found."));
        return toView(contract);
    }

    @Transactional
    public ContractView updateTerms(Long userId, Long contractId, ContractTermsRequest request) {
        Contract contract = brandOwned(userId, contractId);
        ensureEditable(contract);

        contract.setVersionNumber(contract.getVersionNumber() + 1);
        contract.setAgreedCompensation(request.agreedCompensation());
        contract.setCurrency(request.currency().trim().toUpperCase(Locale.ROOT));
        contract.setDeliverablesSummary(trimToNull(request.deliverablesSummary()));
        contract.setCompletionDeadline(request.completionDeadline());
        contract.setUsageRights(trimToNull(request.usageRights()));
        contract.setRevisionAllowance(request.revisionAllowance());
        contract.setCancellationTerms(trimToNull(request.cancellationTerms()));
        contract.setPaymentSchedule(trimToNull(request.paymentSchedule()));
        contract.setStatus(ContractStatus.DRAFT);
        contract.setBrandAcknowledgedAt(null);
        contract.setCreatorAcknowledgedAt(null);

        contract = contractRepository.saveAndFlush(contract);
        versionRepository.save(new ContractVersion(contract, user(userId)));
        return toView(contract);
    }

    @Transactional
    public ContractView send(Long userId, Long contractId) {
        Contract contract = brandOwned(userId, contractId);
        ensureEditable(contract);
        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw Exceptions.conflict("INVALID_STATE_TRANSITION",
                    "Only a draft contract can be sent for acknowledgement.");
        }
        contract.setStatus(ContractStatus.PENDING_ACKNOWLEDGEMENT);
        contract = contractRepository.saveAndFlush(contract);
        String message = "Contract " + contract.getReferenceNumber() + " is awaiting your acknowledgement.";
        notificationService.notify(contract.getBrand().getUser(),
                NotificationType.CONTRACT_AWAITING_ACKNOWLEDGEMENT, "Contract sent for acknowledgement",
                message, "/dashboard.html");
        notificationService.notify(contract.getCreator().getUser(),
                NotificationType.CONTRACT_AWAITING_ACKNOWLEDGEMENT, "Contract awaiting acknowledgement",
                message, "/dashboard.html");
        return toView(contract);
    }

    @Transactional
    public ContractView acknowledge(Long userId, Role role, Long contractId) {
        if (role != Role.BRAND && role != Role.CREATOR) {
            throw Exceptions.forbidden("Only the contracting brand or creator can acknowledge terms.");
        }
        Contract contract = readable(userId, role, contractId);
        ensureOpen(contract.getCollaboration());
        if (contract.getStatus() != ContractStatus.PENDING_ACKNOWLEDGEMENT) {
            throw Exceptions.conflict("CONTRACT_NOT_OPEN_FOR_ACKNOWLEDGEMENT",
                    "This contract is not waiting for acknowledgement.");
        }
        if (acceptanceRepository.existsByContractIdAndVersionNumberAndAcceptedById(
                contract.getId(), contract.getVersionNumber(), userId)) {
            throw Exceptions.conflict("ALREADY_ACKNOWLEDGED",
                    "You have already acknowledged this contract version.");
        }

        User acceptor = user(userId);
        acceptanceRepository.save(new ContractAcceptance(contract, contract.getVersionNumber(),
                acceptor, role));

        Instant now = Instant.now();
        if (role == Role.BRAND) {
            contract.setBrandAcknowledgedAt(now);
        } else {
            contract.setCreatorAcknowledgedAt(now);
        }
        if (contract.getBrandAcknowledgedAt() != null
                && contract.getCreatorAcknowledgedAt() != null) {
            contract.setStatus(ContractStatus.ACKNOWLEDGED);
        }
        contract = contractRepository.saveAndFlush(contract);
        notificationService.notify(contract.getBrand().getUser(),
                NotificationType.CONTRACT_AWAITING_ACKNOWLEDGEMENT, "Contract acknowledgement recorded",
                acceptor.getFullName() + " acknowledged contract " + contract.getReferenceNumber() + ".",
                "/dashboard.html");
        return toView(contract);
    }

    private Contract create(Collaboration collaboration) {
        Contract contract = new Contract(collaboration, generateReference());
        contract.setDeliverablesSummary(collaboration.getCampaign().getDeliverableRequirements());
        contract.setCompletionDeadline(collaboration.getCampaign().getEndDate());
        contract.setUsageRights(DEFAULT_USAGE_RIGHTS);
        contract.setRevisionAllowance(1);
        contract.setCancellationTerms(DEFAULT_CANCELLATION_TERMS);
        contract.setPaymentSchedule(DEFAULT_PAYMENT_SCHEDULE);
        contract = contractRepository.saveAndFlush(contract);
        versionRepository.save(new ContractVersion(contract, collaboration.getBrand().getUser()));
        return contract;
    }

    private void ensureEditable(Contract contract) {
        ensureOpen(contract.getCollaboration());
        if (contract.getStatus() == ContractStatus.ACKNOWLEDGED
                || contract.getStatus() == ContractStatus.VOID) {
            throw Exceptions.conflict("CONTRACT_TERMS_LOCKED",
                    "Acknowledged contract terms are locked. Create a new version through support.");
        }
    }

    private void ensureOpen(Collaboration collaboration) {
        if (collaboration.getStatus() == CollaborationStatus.CANCELLED
                || collaboration.getStatus() == CollaborationStatus.COMPLETED) {
            throw Exceptions.conflict("COLLABORATION_CLOSED",
                    "This collaboration no longer accepts contract changes.");
        }
    }

    private Contract readable(Long userId, Role role, Long contractId) {
        if (role == Role.BRAND) {
            return contractRepository.findByIdAndCollaborationBrandUserId(contractId, userId)
                    .orElseThrow(() -> Exceptions.notFound("Contract not found."));
        }
        if (role == Role.CREATOR) {
            return contractRepository.findByIdAndCollaborationCreatorUserId(contractId, userId)
                    .orElseThrow(() -> Exceptions.notFound("Contract not found."));
        }
        return contractRepository.findById(contractId)
                .orElseThrow(() -> Exceptions.notFound("Contract not found."));
    }

    private Contract brandOwned(Long userId, Long contractId) {
        brandProfileRepository.findByUserId(userId)
                .orElseThrow(() -> Exceptions.notFound("Brand profile not found."));
        return contractRepository.findByIdAndCollaborationBrandUserId(contractId, userId)
                .orElseThrow(() -> Exceptions.notFound("Contract not found."));
    }

    private Collaboration readableCollaboration(Long userId, Role role, Long collaborationId) {
        if (role == Role.BRAND) {
            return collaborationRepository.findByIdAndBrandUserId(collaborationId, userId)
                    .orElseThrow(() -> Exceptions.notFound("Collaboration not found."));
        }
        if (role == Role.CREATOR) {
            return collaborationRepository.findByIdAndCreatorUserId(collaborationId, userId)
                    .orElseThrow(() -> Exceptions.notFound("Collaboration not found."));
        }
        return collaborationRepository.findById(collaborationId)
                .orElseThrow(() -> Exceptions.notFound("Collaboration not found."));
    }

    private User user(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> Exceptions.notFound("Account not found."));
    }

    private ContractView toView(Contract contract) {
        return ContractView.from(contract,
                versionRepository.findByContractIdOrderByVersionNumberDesc(contract.getId()),
                acceptanceRepository.findByContractIdOrderByAcceptedAtDesc(contract.getId()));
    }

    private String generateReference() {
        String suffix = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase(Locale.ROOT);
        return "CN-" + LocalDate.now().getYear() + "-" + suffix;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
