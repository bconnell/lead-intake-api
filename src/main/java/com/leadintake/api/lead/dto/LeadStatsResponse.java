package com.leadintake.api.lead.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LeadStatsResponse(
        long total,
        @JsonProperty("new") long newLeads,
        long contacted,
        long qualified,
        long closed,
        long rejected) {
}
