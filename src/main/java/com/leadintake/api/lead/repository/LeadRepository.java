package com.leadintake.api.lead.repository;

import com.leadintake.api.lead.model.LeadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LeadRepository extends JpaRepository<LeadEntity, UUID> {
}
