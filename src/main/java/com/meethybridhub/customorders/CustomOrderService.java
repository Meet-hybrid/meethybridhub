package com.meethybridhub.customorders;

import com.meethybridhub.common.exception.BadRequestException;
import com.meethybridhub.common.exception.ForbiddenException;
import com.meethybridhub.common.exception.ResourceNotFoundException;
import com.meethybridhub.identity.User;
import com.meethybridhub.identity.UserRepository;
import com.meethybridhub.orders.Order;
import com.meethybridhub.orders.OrderRepository;
import com.meethybridhub.orders.OrderStatus;
import com.meethybridhub.store.StoreService;
import com.meethybridhub.store.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class CustomOrderService {

    private static final Logger log = LoggerFactory.getLogger(CustomOrderService.class);

    private final CustomOrderRequestRepository requestRepository;
    private final QuoteRepository quoteRepository;
    private final CustomOrderConversationRepository conversationRepository;
    private final CustomOrderAttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final StoreService storeService;

    public CustomOrderService(CustomOrderRequestRepository requestRepository,
                               QuoteRepository quoteRepository,
                               CustomOrderConversationRepository conversationRepository,
                               CustomOrderAttachmentRepository attachmentRepository,
                               UserRepository userRepository,
                               OrderRepository orderRepository,
                               StoreService storeService) {
        this.requestRepository = requestRepository;
        this.quoteRepository = quoteRepository;
        this.conversationRepository = conversationRepository;
        this.attachmentRepository = attachmentRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.storeService = storeService;
    }

    // ─── Request CRUD ───────────────────────────────────────────────

    public CustomOrderRequest createRequest(User customer, Long storeId, String title,
                                             String description, BigDecimal budgetMin,
                                             BigDecimal budgetMax, Instant deadline) {
        if (customer.getRoles() == null || !customer.getRoles().contains("CUSTOMER")) {
            // Customers can always create requests; this is a soft check.
        }

        CustomOrderRequest request = new CustomOrderRequest(
                storeId, customer, title.trim(), description.trim(),
                budgetMin, budgetMax, deadline);
        request.setStatus(CustomOrderStatus.OPEN);

        CustomOrderRequest saved = requestRepository.save(request);
        log.info("Custom order request created: {} by user {} for store {}",
                saved.getId(), customer.getId(), storeId);
        return saved;
    }

    public CustomOrderRequest getRequest(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Custom order request not found: " + requestId));
    }

    public List<CustomOrderRequest> listRequestsForStore(Long storeId, String status) {
        if (status != null && !status.isBlank()) {
            CustomOrderStatus cs;
            try {
                cs = CustomOrderStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Unknown status: " + status);
            }
            return requestRepository.findByStoreIdAndStatus(storeId, cs);
        }
        return requestRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
    }

    public List<CustomOrderRequest> listRequestsForCustomer(Long customerId) {
        return requestRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public CustomOrderRequest updateStatus(User actor, Long requestId, CustomOrderStatus newStatus) {
        CustomOrderRequest request = getRequest(requestId);
        ensureStoreAccess(actor, request.getStoreId());

        CustomOrderStatus prev = request.getStatus();
        validateTransition(prev, newStatus);
        request.setStatus(newStatus);

        CustomOrderRequest saved = requestRepository.save(request);
        log.info("Request {} status: {} → {} by user {}", requestId, prev, newStatus, actor.getId());
        return saved;
    }

    // ─── Quotes ─────────────────────────────────────────────────────

    public Quote createQuote(User storeOwner, Long requestId, BigDecimal price,
                              String currency, Integer estimatedDays, String terms,
                              int expiryDays) {
        CustomOrderRequest request = getRequest(requestId);
        ensureStoreAccess(storeOwner, request.getStoreId());

        if (request.getStatus() != CustomOrderStatus.OPEN
                && request.getStatus() != CustomOrderStatus.IN_REVIEW) {
            throw new BadRequestException(
                    "Cannot quote a request in status " + request.getStatus());
        }

        Instant expiresAt = Instant.now().plus(expiryDays, ChronoUnit.DAYS);
        Quote quote = new Quote(
                request.getStoreId(), request, price,
                currency != null ? currency : "NGN",
                estimatedDays, terms, expiresAt);

        Quote saved = quoteRepository.save(quote);

        request.setStatus(CustomOrderStatus.QUOTED);
        requestRepository.save(request);

        log.info("Quote {} created for request {} — {} {} expires {}",
                saved.getId(), requestId, price, currency, expiresAt);
        return saved;
    }

    public Quote acceptQuote(User customer, Long quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new ResourceNotFoundException("Quote not found: " + quoteId));

        CustomOrderRequest request = quote.getRequest();
        if (!request.getCustomerId().equals(customer.getId())) {
            throw new ForbiddenException("Only the requesting customer can accept a quote");
        }
        if (quote.getStatus() != QuoteStatus.PENDING) {
            throw new BadRequestException("Quote is not in PENDING status");
        }
        if (quote.getExpiresAt() != null && quote.getExpiresAt().isBefore(Instant.now())) {
            quote.setStatus(QuoteStatus.EXPIRED);
            quoteRepository.save(quote);
            throw new BadRequestException("Quote has expired");
        }

        quote.setStatus(QuoteStatus.ACCEPTED);
        quoteRepository.save(quote);

        request.setStatus(CustomOrderStatus.ACCEPTED);
        requestRepository.save(request);

        // Reject other pending quotes for the same request
        quoteRepository.findByRequestIdOrderByCreatedAtDesc(request.getId()).stream()
                .filter(q -> !q.getId().equals(quoteId) && q.getStatus() == QuoteStatus.PENDING)
                .forEach(q -> {
                    q.setStatus(QuoteStatus.REJECTED);
                    quoteRepository.save(q);
                });

        log.info("Quote {} accepted by user {} for request {}", quoteId, customer.getId(), request.getId());
        return quote;
    }

    public Quote rejectQuote(User customer, Long quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new ResourceNotFoundException("Quote not found: " + quoteId));

        CustomOrderRequest request = quote.getRequest();
        if (!request.getCustomerId().equals(customer.getId())) {
            throw new ForbiddenException("Only the requesting customer can reject a quote");
        }
        if (quote.getStatus() != QuoteStatus.PENDING) {
            throw new BadRequestException("Quote is not in PENDING status");
        }

        quote.setStatus(QuoteStatus.REJECTED);
        quoteRepository.save(quote);

        log.info("Quote {} rejected by user {} for request {}", quoteId, customer.getId(), request.getId());
        return quote;
    }

    public List<Quote> listQuotesForRequest(Long requestId) {
        return quoteRepository.findByRequestIdOrderByCreatedAtDesc(requestId);
    }

    // ─── Conversation ───────────────────────────────────────────────

    public CustomOrderConversation sendMessage(User sender, Long requestId, String message) {
        CustomOrderRequest request = getRequest(requestId);
        ensureConversationAccess(sender, request);

        CustomOrderConversation msg = new CustomOrderConversation(request, sender, message.trim());
        CustomOrderConversation saved = conversationRepository.save(msg);

        // Move to IN_REVIEW if still OPEN
        if (request.getStatus() == CustomOrderStatus.OPEN) {
            request.setStatus(CustomOrderStatus.IN_REVIEW);
            requestRepository.save(request);
        }

        log.info("Message sent on request {} by user {}", requestId, sender.getId());
        return saved;
    }

    public List<CustomOrderConversation> listMessages(Long requestId) {
        return conversationRepository.findByRequestIdOrderByCreatedAtAsc(requestId);
    }

    // ─── Attachments ────────────────────────────────────────────────

    public CustomOrderAttachment addAttachment(User uploader, Long requestId,
                                                String fileUrl, String fileName,
                                                String fileType, Long fileSizeBytes) {
        CustomOrderRequest request = getRequest(requestId);
        ensureConversationAccess(uploader, request);

        CustomOrderAttachment attachment = new CustomOrderAttachment(
                request, uploader, fileUrl, fileName, fileType, fileSizeBytes);
        return attachmentRepository.save(attachment);
    }

    public List<CustomOrderAttachment> listAttachments(Long requestId) {
        return attachmentRepository.findByRequestIdOrderByCreatedAtAsc(requestId);
    }

    // ─── Convert to Order ───────────────────────────────────────────

    public Order convertToOrder(Long requestId) {
        CustomOrderRequest request = getRequest(requestId);
        if (request.getStatus() != CustomOrderStatus.ACCEPTED) {
            throw new BadRequestException(
                    "Only ACCEPTED requests can be converted to orders");
        }

        Quote acceptedQuote = quoteRepository.findByRequestIdOrderByCreatedAtDesc(request.getId())
                .stream()
                .filter(q -> q.getStatus() == QuoteStatus.ACCEPTED)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No accepted quote found"));

        User customer = userRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        String orderNumber = "COR-" + System.currentTimeMillis();
        Order order = new Order(
                request.getStoreId(), customer, orderNumber,
                customer.getEmail(), "", "",
                "Converted from custom order #" + request.getId() + " — " + request.getTitle());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(acceptedQuote.getPrice());

        Order saved = orderRepository.save(order);

        request.setStatus(CustomOrderStatus.CONVERTED);
        requestRepository.save(request);

        log.info("Custom order request {} converted to order {}", requestId, saved.getId());
        return saved;
    }

    // ─── Helpers ────────────────────────────────────────────────────

    private void ensureStoreAccess(User user, Long storeId) {
        Long resolvedStoreId = TenantContext.getStoreId().orElse(null);
        if (resolvedStoreId != null && resolvedStoreId.equals(storeId)) {
            return; // StoreFilter resolved this tenant — OK
        }
        // Fallback: admin or owner check via StoreService
        storeService.getCurrentTenantStore(user);
    }

    private void ensureConversationAccess(User user, CustomOrderRequest request) {
        boolean isCustomer = request.getCustomerId().equals(user.getId());
        boolean isStoreOwner = false;
        try {
            storeService.getCurrentTenantStore(user);
            isStoreOwner = true;
        } catch (Exception ignored) {
        }
        if (!isCustomer && !isStoreOwner && !user.hasRole("ADMIN")) {
            throw new ForbiddenException("You do not have access to this conversation");
        }
    }

    private void validateTransition(CustomOrderStatus from, CustomOrderStatus to) {
        boolean valid = switch (from) {
            case OPEN -> to == CustomOrderStatus.IN_REVIEW || to == CustomOrderStatus.REJECTED;
            case IN_REVIEW -> to == CustomOrderStatus.QUOTED || to == CustomOrderStatus.REJECTED;
            case QUOTED -> to == CustomOrderStatus.ACCEPTED || to == CustomOrderStatus.REJECTED || to == CustomOrderStatus.EXPIRED;
            case ACCEPTED -> to == CustomOrderStatus.CONVERTED;
            default -> false;
        };
        if (!valid) {
            throw new BadRequestException("Invalid status transition: " + from + " → " + to);
        }
    }
}
