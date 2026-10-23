package com.collabnexus.common;

import java.net.URI;
import java.net.URISyntaxException;

public final class Urls {

    private Urls() {
    }

    public static String normalizeHttpUrl(String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String candidate = value.trim();
        try {
            URI uri = new URI(candidate);
            String scheme = uri.getScheme();
            boolean http = scheme != null
                    && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
            if (!http || uri.getHost() == null) {
                throw Exceptions.badRequest("INVALID_URL",
                        label + " must be a full URL starting with http:// or https://.");
            }
            return candidate;
        } catch (URISyntaxException ex) {
            throw Exceptions.badRequest("INVALID_URL",
                    label + " must be a full URL starting with http:// or https://.");
        }
    }
}
