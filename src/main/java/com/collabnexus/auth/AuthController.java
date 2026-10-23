package com.collabnexus.auth;

import com.collabnexus.admin.PlatformSettingsService;
import com.collabnexus.common.ApiResponse;
import com.collabnexus.common.Exceptions;
import com.collabnexus.security.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final AuthRateLimiter rateLimiter;
    private final PlatformSettingsService settingsService;

    public AuthController(AuthService authService,
                          AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          SessionAuthenticationStrategy sessionAuthenticationStrategy,
                          AuthRateLimiter rateLimiter,
                          PlatformSettingsService settingsService) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.rateLimiter = rateLimiter;
        this.settingsService = settingsService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SessionView> register(@Valid @RequestBody RegisterRequest request,
                                             HttpServletRequest httpRequest,
                                             HttpServletResponse httpResponse) {
        String emailScope = normalizeEmail(request.email());
        rateLimiter.check("register:ip:" + clientKey(httpRequest), 5, Duration.ofHours(1));
        rateLimiter.check("register:email:" + emailScope, 3, Duration.ofHours(1));
        if (!settingsService.getBoolean(PlatformSettingsService.REGISTRATION_OPEN)) {
            throw Exceptions.forbidden("Registration is currently closed. Please contact support.");
        }
        SessionView created = authService.register(request);
        Authentication authentication = authenticate(request.email(), request.password());
        establishSession(authentication, httpRequest, httpResponse);
        return ApiResponse.ok(authService.sessionFor(created.user().id()), "Your account is ready.");
    }

    @PostMapping("/login")
    public ApiResponse<SessionView> login(@Valid @RequestBody LoginRequest request,
                                          HttpServletRequest httpRequest,
                                          HttpServletResponse httpResponse) {
        String ipKey = "login:ip:" + clientKey(httpRequest);
        String emailKey = "login:email:" + normalizeEmail(request.email());
        rateLimiter.check(ipKey, 20, Duration.ofMinutes(5));
        rateLimiter.check(emailKey, 10, Duration.ofMinutes(5));
        Authentication authentication = authenticate(request.email(), request.password());
        rateLimiter.reset(ipKey);
        rateLimiter.reset(emailKey);
        establishSession(authentication, httpRequest, httpResponse);
        SecurityUser principal = (SecurityUser) authentication.getPrincipal();
        return ApiResponse.ok(authService.sessionFor(principal.getId()), "Signed in successfully.");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response,
                                    Authentication authentication) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ApiResponse.ok(null, "Signed out.");
    }

    @GetMapping("/me")
    public ApiResponse<SessionView> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser principal)) {
            throw Exceptions.unauthorized("Please sign in to continue.");
        }
        return ApiResponse.ok(authService.sessionFor(principal.getId()));
    }

    @GetMapping("/csrf")
    public ApiResponse<CsrfView> csrf(CsrfToken token) {
        return ApiResponse.ok(new CsrfView(token.getHeaderName(), token.getParameterName(), token.getToken()));
    }

    private Authentication authenticate(String email, String password) {
        try {
            return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    email.trim().toLowerCase(Locale.ROOT), password));
        } catch (DisabledException ex) {
            throw Exceptions.unauthorized("This account is suspended. Please contact support.");
        } catch (AuthenticationException ex) {
            throw Exceptions.unauthorized("Email or password is incorrect.");
        }
    }

    private void establishSession(Authentication authentication, HttpServletRequest request,
                                  HttpServletResponse response) {
        request.getSession(true);
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private String clientKey(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record CsrfView(String headerName, String parameterName, String token) {
    }
}
