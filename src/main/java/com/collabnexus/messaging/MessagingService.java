package com.collabnexus.messaging;

import com.collabnexus.brands.BrandProfileRepository;
import com.collabnexus.collaborations.Collaboration;
import com.collabnexus.collaborations.CollaborationRepository;
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

import java.time.Instant;
import java.util.Locale;

@Service
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final CollaborationRepository collaborationRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public MessagingService(ConversationRepository conversationRepository,
                            ConversationMessageRepository messageRepository,
                            CollaborationRepository collaborationRepository,
                            BrandProfileRepository brandProfileRepository,
                            CreatorProfileRepository creatorProfileRepository,
                            UserRepository userRepository,
                            NotificationService notificationService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.collaborationRepository = collaborationRepository;
        this.brandProfileRepository = brandProfileRepository;
        this.creatorProfileRepository = creatorProfileRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ConversationView ensureForCollaboration(Long userId, Role role, Long collaborationId) {
        Collaboration collaboration = participantCollaboration(userId, role, collaborationId);
        Conversation conversation = conversationRepository.findByCollaborationId(collaborationId)
                .orElseGet(() -> conversationRepository.saveAndFlush(
                        new Conversation(collaboration, collaboration.getCampaign().getTitle())));
        return toView(conversation, userId);
    }

    @Transactional
    public ConversationView create(Long userId, Role role, ConversationRequest request) {
        return ensureForCollaboration(userId, role, request.collaborationId());
    }

    @Transactional(readOnly = true)
    public PageView<ConversationView> list(Long userId, Role role, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Conversation> results;
        if (role == Role.BRAND) {
            brandProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> Exceptions.notFound("Brand profile not found."));
            results = conversationRepository.findByBrandUserId(userId, pageRequest);
        } else if (role == Role.CREATOR) {
            creatorProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> Exceptions.notFound("Creator profile not found."));
            results = conversationRepository.findByCreatorUserId(userId, pageRequest);
        } else {
            results = conversationRepository.findAll(pageRequest);
        }
        return PageView.of(results, conversation -> toView(conversation, userId));
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId, Role role) {
        if (role != Role.BRAND && role != Role.CREATOR) {
            return 0;
        }
        Page<Conversation> conversations = role == Role.BRAND
                ? conversationRepository.findByBrandUserId(userId, PageRequest.of(0, 100))
                : conversationRepository.findByCreatorUserId(userId, PageRequest.of(0, 100));
        long total = 0;
        for (Conversation conversation : conversations.getContent()) {
            total += messageRepository.countByConversationIdAndSenderIdNotAndReadAtIsNull(
                    conversation.getId(), userId);
        }
        return total;
    }

    @Transactional
    public PageView<MessageView> messages(Long userId, Role role, Long conversationId, int page, int size) {
        Conversation conversation = readable(userId, role, conversationId);
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<ConversationMessage> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId(), pageRequest);
        if (role == Role.BRAND || role == Role.CREATOR) {
            messageRepository.markRead(conversationId, userId, Instant.now());
        }
        return PageView.of(messages, MessageView::from);
    }

    @Transactional
    public MessageView send(Long userId, Role role, Long conversationId, MessageRequest request) {
        Conversation conversation = readable(userId, role, conversationId);
        if (role != Role.BRAND && role != Role.CREATOR) {
            throw Exceptions.forbidden("Only conversation participants can send messages.");
        }
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> Exceptions.notFound("Account not found."));
        ConversationMessage message = messageRepository.saveAndFlush(
                new ConversationMessage(conversation, sender, request.body().trim()));
        conversation.setLastMessageAt(message.getCreatedAt());
        conversationRepository.saveAndFlush(conversation);

        Long recipientId = role == Role.BRAND
                ? conversation.getCreator().getUser().getId()
                : conversation.getBrand().getUser().getId();
        notificationService.notify(recipientId, NotificationType.NEW_MESSAGE,
                "New message from " + sender.getFullName(),
                preview(message.getBody()), "/dashboard.html");

        return MessageView.from(message);
    }

    @Transactional
    public int markRead(Long userId, Role role, Long conversationId) {
        readable(userId, role, conversationId);
        if (role != Role.BRAND && role != Role.CREATOR) {
            return 0;
        }
        return messageRepository.markRead(conversationId, userId, Instant.now());
    }

    private Conversation readable(Long userId, Role role, Long conversationId) {
        if (role == Role.BRAND) {
            return conversationRepository.findByIdAndBrandUserId(conversationId, userId)
                    .orElseThrow(() -> Exceptions.notFound("Conversation not found."));
        }
        if (role == Role.CREATOR) {
            return conversationRepository.findByIdAndCreatorUserId(conversationId, userId)
                    .orElseThrow(() -> Exceptions.notFound("Conversation not found."));
        }
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> Exceptions.notFound("Conversation not found."));
    }

    private Collaboration participantCollaboration(Long userId, Role role, Long collaborationId) {
        if (role == Role.BRAND) {
            brandProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> Exceptions.notFound("Brand profile not found."));
            return collaborationRepository.findByIdAndBrandUserId(collaborationId, userId)
                    .orElseThrow(() -> Exceptions.notFound("Collaboration not found."));
        }
        if (role == Role.CREATOR) {
            creatorProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> Exceptions.notFound("Creator profile not found."));
            return collaborationRepository.findByIdAndCreatorUserId(collaborationId, userId)
                    .orElseThrow(() -> Exceptions.notFound("Collaboration not found."));
        }
        throw Exceptions.forbidden("Only collaboration participants can start a conversation.");
    }

    private ConversationView toView(Conversation conversation, Long userId) {
        ConversationMessage last = messageRepository
                .findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId()).orElse(null);
        long unread = messageRepository.countByConversationIdAndSenderIdNotAndReadAtIsNull(
                conversation.getId(), userId);
        return new ConversationView(
                conversation.getId(),
                conversation.getCollaboration().getId(),
                conversation.getCollaboration().getCampaign().getTitle(),
                conversation.getBrand().getCompanyName(),
                conversation.getCreator().getDisplayName(),
                conversation.getSubject(),
                last == null ? null : preview(last.getBody()),
                conversation.getLastMessageAt(),
                unread,
                conversation.getUpdatedAt()
        );
    }

    private String preview(String body) {
        String flat = body.replace('\n', ' ').trim();
        return flat.length() > 120 ? flat.substring(0, 117) + "..." : flat;
    }
}
