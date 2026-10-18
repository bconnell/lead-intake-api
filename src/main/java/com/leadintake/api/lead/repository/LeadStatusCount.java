package com.leadintake.api.lead.repository;

import com.leadintake.api.lead.model.LeadStatus;

public record LeadStatusCount(LeadStatus status, long count) {
}
