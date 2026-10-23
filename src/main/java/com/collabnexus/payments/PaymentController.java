package com.collabnexus.payments;

import com.collabnexus.common.ApiResponse;
import com.collabnexus.common.PageView;
import com.collabnexus.security.SecurityUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/api/payments")
    public ApiResponse<PageView<PaymentView>> list(@AuthenticationPrincipal SecurityUser user,
                                                   @RequestParam(required = false) PaymentStatus status,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "8") int size) {
        return ApiResponse.ok(paymentService.list(user.getId(), user.getRole(), status, page, size));
    }

    @GetMapping("/api/payments/summary")
    public ApiResponse<PaymentSummary> summary(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(paymentService.summary(user.getId(), user.getRole()));
    }

    @GetMapping("/api/payments/{id}")
    public ApiResponse<PaymentView> detail(@AuthenticationPrincipal SecurityUser user,
                                           @PathVariable Long id) {
        return ApiResponse.ok(paymentService.detail(user.getId(), user.getRole(), id));
    }

    @PostMapping("/api/brand/collaborations/{collaborationId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PaymentView> create(@AuthenticationPrincipal SecurityUser user,
                                           @PathVariable Long collaborationId,
                                           @Valid @RequestBody PaymentRequest request) {
        return ApiResponse.ok(paymentService.create(user.getId(), collaborationId, request),
                "Payment milestone recorded. No money has been transferred.");
    }

    @PostMapping("/api/payments/{id}/status")
    public ApiResponse<PaymentView> updateStatus(@AuthenticationPrincipal SecurityUser user,
                                                 @PathVariable Long id,
                                                 @Valid @RequestBody PaymentStatusRequest request) {
        return ApiResponse.ok(paymentService.updateStatus(user.getId(), user.getRole(), id, request),
                "Payment status updated. This record does not verify an external transfer.");
    }
}
