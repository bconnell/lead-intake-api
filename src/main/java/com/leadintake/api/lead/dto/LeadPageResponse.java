package com.leadintake.api.lead.dto;

import com.leadintake.api.lead.model.LeadEntity;
import org.springframework.data.domain.Page;

import java.util.List;

public record LeadPageResponse(
        List<LeadResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static LeadPageResponse from(Page<LeadEntity> leads) {
        List<LeadResponse> content = leads.getContent().stream()
                .map(LeadResponse::from)
                .toList();
        return new LeadPageResponse(
                content,
                leads.getNumber(),
                leads.getSize(),
                leads.getTotalElements(),
                leads.getTotalPages());
    }
}
