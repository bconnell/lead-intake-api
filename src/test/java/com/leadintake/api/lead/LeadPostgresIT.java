package com.leadintake.api.lead;

import com.leadintake.api.lead.model.LeadStatus;
import com.leadintake.api.lead.model.LeadEntity;
import com.leadintake.api.lead.repository.LeadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class LeadPostgresIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private LeadRepository leadRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearLeads() {
        leadRepository.deleteAllInBatch();
    }

    @Test
    void appliesFlywayMigrationsAndStartsWithValidatedJpaMapping() {
        Long appliedMigrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version IN ('1', '2') AND success = TRUE",
                Long.class);

        assertThat(appliedMigrations).isEqualTo(2L);
    }

    @Test
    void persistsLeadsAndEnforcesNormalizedEmailUniqueness() throws Exception {
        String email = "postgres-" + UUID.randomUUID() + "@example.test";

        MvcResult created = mockMvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest("Postgres Lead", email)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("/api/leads/")))
                .andExpect(jsonPath("$.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.status").value(LeadStatus.NEW.name()))
                .andReturn();

        UUID id = leadIdFrom(created);
        assertThat(leadRepository.findById(id)).isPresent();
        mockMvc.perform(get("/api/leads/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value(email.toLowerCase()));

        mockMvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest("Duplicate Postgres Lead", email.toUpperCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(leadRepository.count()).isEqualTo(1L);
    }

    @Test
    void updatesPagesCountsAndDeletesLeadsThroughPostgresBackedHttpRoutes() throws Exception {
        UUID firstQualifiedId = createLead("first-" + UUID.randomUUID() + "@example.test");
        UUID secondQualifiedId = createLead("second-" + UUID.randomUUID() + "@example.test");
        createLead("contacted-" + UUID.randomUUID() + "@example.test");

        mockMvc.perform(patch("/api/leads/{id}/status", firstQualifiedId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"QUALIFIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUALIFIED"));

        mockMvc.perform(patch("/api/leads/{id}/status", secondQualifiedId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"QUALIFIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUALIFIED"));

        mockMvc.perform(get("/api/leads")
                        .param("status", "QUALIFIED")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        clearInvocations(leadRepository);
        mockMvc.perform(get("/api/leads/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$['new']").value(1))
                .andExpect(jsonPath("$.contacted").value(0))
                .andExpect(jsonPath("$.qualified").value(2))
                .andExpect(jsonPath("$.closed").value(0))
                .andExpect(jsonPath("$.rejected").value(0));

        verify(leadRepository).countByStatusGrouped();
        verify(leadRepository, never()).count();

        mockMvc.perform(delete("/api/leads/{id}", firstQualifiedId))
                .andExpect(status().isNoContent());
        assertThat(leadRepository.findById(firstQualifiedId)).isEmpty();
    }


    @Test
    void returnsZeroForEveryStatusWhenPostgresHasNoLeads() throws Exception {
        clearInvocations(leadRepository);

        mockMvc.perform(get("/api/leads/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$['new']").value(0))
                .andExpect(jsonPath("$.contacted").value(0))
                .andExpect(jsonPath("$.qualified").value(0))
                .andExpect(jsonPath("$.closed").value(0))
                .andExpect(jsonPath("$.rejected").value(0));

        verify(leadRepository).countByStatusGrouped();
        verify(leadRepository, never()).count();
    }

    @Test
    void preservesSafeProblemDetailsForPostgresCheckConstraintViolations() throws Exception {
        DataIntegrityViolationException constraintFailure = assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        "INSERT INTO leads (id, name, email, status, created_at, updated_at) "
                                + "VALUES (?, ?, ?, ?, ?, ?)",
                        UUID.randomUUID(),
                        "Invalid Check Constraint",
                        "invalid-check-" + UUID.randomUUID() + "@example.test",
                        "NOT_A_SUPPORTED_STATUS",
                        OffsetDateTime.now(),
                        OffsetDateTime.now()));

        // Use the actual PostgreSQL integrity exception rather than an invented message.
        doThrow(constraintFailure).when(leadRepository).save(any(LeadEntity.class));

        mockMvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(
                                "Valid Postgres Lead",
                                "unexpected-" + UUID.randomUUID() + "@example.test")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."));

        assertThat(leadRepository.count()).isZero();
    }

    private UUID createLead(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest("Postgres Integration Lead", email)))
                .andExpect(status().isCreated())
                .andReturn();
        return leadIdFrom(result);
    }

    private String createRequest(String name, String email) {
        return """
                {"name":"%s","email":"%s"}
                """.formatted(name, email);
    }

    private UUID leadIdFrom(MvcResult result) {
        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(location).startsWith("/api/leads/");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }
}
