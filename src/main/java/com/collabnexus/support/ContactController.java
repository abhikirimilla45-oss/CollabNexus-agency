package com.collabnexus.support;

import com.collabnexus.auth.AuthRateLimiter;
import com.collabnexus.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/public")
public class ContactController {

    private final ContactService contactService;
    private final AuthRateLimiter rateLimiter;

    public ContactController(ContactService contactService, AuthRateLimiter rateLimiter) {
        this.contactService = contactService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/contact")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ContactService.ContactReceipt> submit(@Valid @RequestBody ContactRequest request,
                                                             HttpServletRequest httpRequest) {
        rateLimiter.check("contact:" + clientKey(httpRequest), 5, Duration.ofHours(1));
        return ApiResponse.ok(contactService.submit(request),
                "Thanks for reaching out. A platform administrator will review your message.");
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
