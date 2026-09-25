package com.leadintake.api.lead.repository;

import com.leadintake.api.lead.model.LeadEntity;
import com.leadintake.api.lead.model.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LeadRepository extends JpaRepository<LeadEntity, UUID> {

    Page<LeadEntity> findByStatus(LeadStatus status, Pageable pageable);

    long countByStatus(LeadStatus status);
}
