package com.leadintake.api.lead.service;

import com.leadintake.api.lead.dto.CreateLeadRequest;
import com.leadintake.api.lead.dto.LeadPageResponse;
import com.leadintake.api.lead.dto.LeadResponse;
import com.leadintake.api.lead.model.LeadEntity;
import com.leadintake.api.lead.model.LeadStatus;
import com.leadintake.api.lead.repository.LeadRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LeadService {

    private final LeadRepository leadRepository;

    public LeadService(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    @Transactional
    public LeadResponse create(CreateLeadRequest request) {
        LeadEntity lead = new LeadEntity(request.name(), request.email(), request.phone());
        return LeadResponse.from(leadRepository.save(lead));
    }

    @Transactional(readOnly = true)
    public LeadResponse find(UUID id) {
        LeadEntity lead = leadRepository.findById(id).orElseThrow(LeadNotFoundException::new);
        return LeadResponse.from(lead);
    }

    @Transactional(readOnly = true)
    public LeadPageResponse list(int page, int size, LeadStatus status) {
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<LeadEntity> leads = status == null
                ? leadRepository.findAll(pageable)
                : leadRepository.findByStatus(status, pageable);
        return LeadPageResponse.from(leads);
    }
}
