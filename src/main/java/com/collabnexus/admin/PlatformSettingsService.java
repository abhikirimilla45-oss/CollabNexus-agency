package com.collabnexus.admin;

import com.collabnexus.common.Exceptions;
import com.collabnexus.moderation.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class PlatformSettingsService {

    public static final String REGISTRATION_OPEN = "registration.open";
    public static final String MARKETPLACE_STATUS = "marketplace.status";
    public static final String DEFAULT_CURRENCY = "default.currency";
    public static final String SUPPORT_EMAIL = "support.email";

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Set<String> MARKETPLACE_STATUSES = Set.of("AVAILABLE", "LIMITED", "READ_ONLY");

    private static final Map<String, Definition> DEFINITIONS = new LinkedHashMap<>();

    static {
        DEFINITIONS.put(REGISTRATION_OPEN, new Definition(REGISTRATION_OPEN, "true",
                "Allow new brand and creator registrations."));
        DEFINITIONS.put(MARKETPLACE_STATUS, new Definition(MARKETPLACE_STATUS, "AVAILABLE",
                "Public availability marker: AVAILABLE, LIMITED, or READ_ONLY."));
        DEFINITIONS.put(DEFAULT_CURRENCY, new Definition(DEFAULT_CURRENCY, "USD",
                "Fallback three-letter currency code for new campaigns."));
        DEFINITIONS.put(SUPPORT_EMAIL, new Definition(SUPPORT_EMAIL, "support@collabnexus.example",
                "Public support contact shown to users."));
    }

    private final PlatformSettingRepository repository;
    private final AuditLogService auditLogService;

    public PlatformSettingsService(PlatformSettingRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public String get(String key) {
        Definition definition = definition(key);
        return repository.findBySettingKeyIgnoreCase(key)
                .map(PlatformSetting::getSettingValue)
                .filter(StringUtils::hasText)
                .orElse(definition.defaultValue());
    }

    @Transactional(readOnly = true)
    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    @Transactional(readOnly = true)
    public List<SettingView> list() {
        Map<String, PlatformSetting> stored = new LinkedHashMap<>();
        repository.findAll().forEach(setting ->
                stored.put(setting.getSettingKey().toLowerCase(Locale.ROOT), setting));
        return DEFINITIONS.values().stream()
                .map(definition -> {
                    PlatformSetting setting = stored.get(definition.key());
                    String value = setting == null || !StringUtils.hasText(setting.getSettingValue())
                            ? definition.defaultValue() : setting.getSettingValue();
                    return new SettingView(definition.key(), value, definition.defaultValue(),
                            definition.description(), setting == null ? null : setting.getUpdatedAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, String> publicSettings() {
        Map<String, String> values = new LinkedHashMap<>();
        DEFINITIONS.keySet().forEach(key -> values.put(key, get(key)));
        return values;
    }

    @Transactional
    public SettingView update(String key, String rawValue, Long actorId) {
        Definition definition = definition(key);
        String value = normalize(definition, rawValue);
        PlatformSetting setting = repository.findBySettingKeyIgnoreCase(key)
                .orElseGet(() -> new PlatformSetting(definition.key(), value, definition.description()));
        String previous = setting.getId() == null ? definition.defaultValue() : setting.getSettingValue();
        setting.setSettingValue(value);
        repository.saveAndFlush(setting);
        auditLogService.record(actorId, "PLATFORM_SETTING_UPDATED", "PlatformSetting", setting.getId(),
                definition.key() + " changed from \"" + previous + "\" to \"" + value + "\".");
        return new SettingView(definition.key(), value, definition.defaultValue(),
                definition.description(), setting.getUpdatedAt());
    }

    private Definition definition(String key) {
        Definition definition = DEFINITIONS.get(key);
        if (definition == null) {
            throw Exceptions.notFound("Platform setting not found.");
        }
        return definition;
    }

    private String normalize(Definition definition, String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            throw Exceptions.badRequest("INVALID_SETTING", "A setting value is required.");
        }
        String value = rawValue.trim();
        switch (definition.key()) {
            case REGISTRATION_OPEN -> {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw Exceptions.badRequest("INVALID_SETTING", "Registration must be true or false.");
                }
                return value.toLowerCase(Locale.ROOT);
            }
            case MARKETPLACE_STATUS -> {
                String status = value.toUpperCase(Locale.ROOT);
                if (!MARKETPLACE_STATUSES.contains(status)) {
                    throw Exceptions.badRequest("INVALID_SETTING",
                            "Marketplace status must be AVAILABLE, LIMITED, or READ_ONLY.");
                }
                return status;
            }
            case DEFAULT_CURRENCY -> {
                String currency = value.toUpperCase(Locale.ROOT);
                if (!currency.matches("^[A-Z]{3}$")) {
                    throw Exceptions.badRequest("INVALID_SETTING",
                            "Default currency must be a three-letter code.");
                }
                return currency;
            }
            case SUPPORT_EMAIL -> {
                if (value.length() > 180 || !EMAIL.matcher(value).matches()) {
                    throw Exceptions.badRequest("INVALID_SETTING", "Support email is not valid.");
                }
                return value;
            }
            default -> {
                if (value.length() > 2000) {
                    throw Exceptions.badRequest("INVALID_SETTING", "The value is too long.");
                }
                return value;
            }
        }
    }

    private record Definition(String key, String defaultValue, String description) {
    }

    public record SettingView(String key, String value, String defaultValue, String description,
                              Instant updatedAt) {
    }
}
