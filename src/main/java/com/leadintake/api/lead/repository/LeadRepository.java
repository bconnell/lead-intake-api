package com.leadintake.api.lead.repository;

import com.leadintake.api.lead.model.LeadEntity;
import com.leadintake.api.lead.model.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<LeadEntity, UUID> {

    Page<LeadEntity> findByStatus(LeadStatus status, Pageable pageable);

    @Query("""
            SELECT new com.leadintake.api.lead.repository.LeadStatusCount(
                l.status, COUNT(l))
            FROM LeadEntity l
            GROUP BY l.status
            """)
    List<LeadStatusCount> countByStatusGrouped();
}
