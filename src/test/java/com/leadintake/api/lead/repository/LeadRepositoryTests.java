package com.leadintake.api.lead.repository;

import com.leadintake.api.lead.model.LeadEntity;
import com.leadintake.api.lead.model.LeadStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LeadRepositoryTests {

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliesTheInitialLeadSchema() {
        Integer appliedMigrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version IN ('1', '2', '3') AND success = TRUE",
                Integer.class);

        assertThat(appliedMigrations).isEqualTo(3);
    }

    @Test
    void savesLeadWithGeneratedUuidInitialStatusAndTimestamps() {
        String email = "lead-" + UUID.randomUUID() + "@example.test";
        LeadEntity saved = leadRepository.saveAndFlush(
                new LeadEntity("Ada Lovelace", email, "+1-555-0100"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(LeadStatus.NEW);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        LeadEntity reloaded = leadRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Ada Lovelace");
        assertThat(reloaded.getEmail()).isEqualTo(email);
        assertThat(reloaded.getPhone()).isEqualTo("+1-555-0100");
        assertThat(reloaded.getStatus()).isEqualTo(LeadStatus.NEW);
        assertThat(reloaded.getCreatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(reloaded.getUpdatedAt()).isEqualTo(saved.getUpdatedAt());
    }

    @Test
    void rejectsStaleLeadStatusWriteAfterAnotherWriterAdvancesItsVersion() {
        LeadEntity lead = leadRepository.saveAndFlush(
                new LeadEntity("Concurrent Test", "stale-" + UUID.randomUUID() + "@example.test", null));
        UUID id = lead.getId();

        int changed = jdbcTemplate.update(
                "UPDATE leads SET status = 'CONTACTED', version = version + 1 WHERE id = ?", id);
        assertThat(changed).isEqualTo(1);

        // The JPA entity still carries the older version. Its delayed
        // mutation must not silently overwrite the newer database value.
        lead.changeStatus(LeadStatus.QUALIFIED);
        assertThatThrownBy(() -> leadRepository.saveAndFlush(lead))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void databaseEnforcesEmailUniqueness() {
        String email = "duplicate-" + UUID.randomUUID() + "@example.test";
        leadRepository.saveAndFlush(new LeadEntity("First Lead", email, null));

        assertThatThrownBy(() ->
                leadRepository.saveAndFlush(new LeadEntity("Second Lead", email, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
