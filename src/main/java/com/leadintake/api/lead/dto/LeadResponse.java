package com.leadintake.api.lead.dto;

import com.leadintake.api.lead.model.LeadEntity;
import com.leadintake.api.lead.model.LeadStatus;

import java.time.Instant;
import java.util.UUID;

public record LeadResponse(
        UUID id,
        String name,
        String email,
        String phone,
        LeadStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static LeadResponse from(LeadEntity lead) {
        return new LeadResponse(
                lead.getId(),
                lead.getName(),
                lead.getEmail(),
                lead.getPhone(),
                lead.getStatus(),
                lead.getCreatedAt(),
                lead.getUpdatedAt());
    }
}
