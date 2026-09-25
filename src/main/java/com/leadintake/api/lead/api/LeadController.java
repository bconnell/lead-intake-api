package com.leadintake.api.lead.api;

import com.leadintake.api.lead.dto.CreateLeadRequest;
import com.leadintake.api.lead.dto.LeadPageResponse;
import com.leadintake.api.lead.dto.LeadResponse;
import com.leadintake.api.lead.dto.LeadStatsResponse;
import com.leadintake.api.lead.dto.UpdateLeadStatusRequest;
import com.leadintake.api.lead.model.LeadStatus;
import com.leadintake.api.lead.service.LeadService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @PostMapping
    public ResponseEntity<LeadResponse> create(@Valid @RequestBody CreateLeadRequest request) {
        LeadResponse lead = leadService.create(request);
        URI location = URI.create("/api/leads/" + lead.id());
        return ResponseEntity.created(location).body(lead);
    }

    @GetMapping
    public LeadPageResponse list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) LeadStatus status) {
        return leadService.list(page, size, status);
    }

    @GetMapping("/{id}")
    public LeadResponse find(@PathVariable UUID id) {
        return leadService.find(id);
    }

    @PatchMapping("/{id}/status")
    public LeadResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLeadStatusRequest request) {
        return leadService.updateStatus(id, request);
    }

    @GetMapping("/stats")
    public LeadStatsResponse stats() {
        return leadService.stats();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        leadService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
