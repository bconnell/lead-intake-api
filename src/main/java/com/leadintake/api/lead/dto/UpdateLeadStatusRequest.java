package com.leadintake.api.lead.dto;

import com.leadintake.api.lead.model.LeadStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateLeadStatusRequest(@NotNull LeadStatus status) {
}
