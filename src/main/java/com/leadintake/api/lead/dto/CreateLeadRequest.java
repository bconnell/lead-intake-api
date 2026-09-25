package com.leadintake.api.lead.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record CreateLeadRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Email @Size(max = 320) String email,
        @Size(max = 32) String phone) {

    public CreateLeadRequest {
        name = stripToNull(name);
        email = normalizeEmail(email);
        phone = stripToNull(phone);
    }

    private static String normalizeEmail(String value) {
        String normalized = stripToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String stripToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
