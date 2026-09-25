package com.leadintake.api.lead;

import com.leadintake.api.lead.model.LeadStatus;
import com.leadintake.api.lead.repository.LeadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Autowired
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

        mockMvc.perform(get("/api/leads/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.qualified").value(2))
                .andExpect(jsonPath("$['new']").value(1));

        mockMvc.perform(delete("/api/leads/{id}", firstQualifiedId))
                .andExpect(status().isNoContent());
        assertThat(leadRepository.findById(firstQualifiedId)).isEmpty();
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
