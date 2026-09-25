package com.leadintake.api.lead;

import com.leadintake.api.lead.model.LeadEntity;
import com.leadintake.api.lead.model.LeadStatus;
import com.leadintake.api.lead.repository.LeadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LeadApiTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private LeadRepository leadRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsLeadAndReturnsItsLocationAndPersistedFields() throws Exception {
        String email = "ada-" + UUID.randomUUID() + "@example.test";

        mockMvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest("  Ada Lovelace  ", email.toUpperCase(), " +1-555-0100 ")))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/leads/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Ada Lovelace"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.phone").value("+1-555-0100"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void retrievesCreatedLeadByItsLocation() throws Exception {
        String email = "grace-" + UUID.randomUUID() + "@example.test";
        MvcResult created = mockMvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest("Grace Hopper", email, null)))
                .andExpect(status().isCreated())
                .andReturn();

        String location = created.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(location).isNotBlank();

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Grace Hopper"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void updatesLeadStatusAndRefreshesUpdatedAt() throws Exception {
        UUID id = UUID.randomUUID();
        OffsetDateTime previousTimestamp = OffsetDateTime.parse("2020-01-02T03:04:05Z");
        insertLead(id, "status-update-" + id + "@example.test", LeadStatus.NEW, previousTimestamp);

        mockMvc.perform(patch("/api/leads/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"QUALIFIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("QUALIFIED"))
                .andExpect(jsonPath("$.createdAt").value(previousTimestamp.toInstant().toString()))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        assertThat(leadRepository.findById(id).orElseThrow().getUpdatedAt())
                .isAfter(previousTimestamp.toInstant());

        mockMvc.perform(get("/api/leads/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUALIFIED"));
    }

    @ParameterizedTest
    @EnumSource(LeadStatus.class)
    void updatesLeadToEverySupportedStatus(LeadStatus targetStatus) throws Exception {
        UUID id = UUID.randomUUID();
        insertLead(id, "valid-status-" + id + "@example.test", LeadStatus.NEW,
                OffsetDateTime.parse("2020-01-02T03:04:05Z"));

        mockMvc.perform(patch("/api/leads/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + targetStatus.name() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(targetStatus.name()));

        assertThat(leadRepository.findById(id).orElseThrow().getStatus()).isEqualTo(targetStatus);
    }

    @Test
    void returnsNotFoundWhenUpdatingMissingLeadStatus() throws Exception {
        mockMvc.perform(patch("/api/leads/{id}/status", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONTACTED\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Lead not found."));
    }

    @Test
    void rejectsNullLeadStatusWithSafeValidationProblem() throws Exception {
        mockMvc.perform(patch("/api/leads/{id}/status", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed."));
    }

    @Test
    void rejectsUnknownLeadStatusWithSafeBadRequest() throws Exception {
        mockMvc.perform(patch("/api/leads/{id}/status", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail")
                        .value("Request body is malformed or contains invalid values."));
    }

    @Test
    void returnsSafeNotFoundProblemForUnknownLead() throws Exception {
        mockMvc.perform(get("/api/leads/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Lead not found."));
    }

    @Test
    void returnsSafeBadRequestForMalformedIdentifier() throws Exception {
        mockMvc.perform(get("/api/leads/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Invalid request parameter."));
    }

    @Test
    void listsLeadsWithDefaultPageMetadata() throws Exception {
        mockMvc.perform(get("/api/leads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void returnsEmptyPageMetadataBeyondLastPage() throws Exception {
        mockMvc.perform(get("/api/leads")
                        .param("page", "10000")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(10000))
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @Transactional
    void reportsTotalAndPerStatusCountsIncludingZeroes() throws Exception {
        leadRepository.deleteAllInBatch();
        saveLeadInStatus(LeadStatus.NEW);
        saveLeadInStatus(LeadStatus.QUALIFIED);
        saveLeadInStatus(LeadStatus.QUALIFIED);
        saveLeadInStatus(LeadStatus.CLOSED);

        clearInvocations(leadRepository);
        mockMvc.perform(get("/api/leads/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$['new']").value(1))
                .andExpect(jsonPath("$.contacted").value(0))
                .andExpect(jsonPath("$.qualified").value(2))
                .andExpect(jsonPath("$.closed").value(1))
                .andExpect(jsonPath("$.rejected").value(0));

        verify(leadRepository, never()).findAll();
        verify(leadRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void reportsZeroForEveryStatisticWhenThereAreNoLeads() throws Exception {
        leadRepository.deleteAllInBatch();

        mockMvc.perform(get("/api/leads/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$['new']").value(0))
                .andExpect(jsonPath("$.contacted").value(0))
                .andExpect(jsonPath("$.qualified").value(0))
                .andExpect(jsonPath("$.closed").value(0))
                .andExpect(jsonPath("$.rejected").value(0));
    }

    @Test
    @Transactional
    void filtersLeadsByStatusAndReturnsDatabasePageMetadata() throws Exception {
        LeadEntity first = new LeadEntity("Contacted One", "contacted-1-" + UUID.randomUUID() + "@example.test", null);
        first.changeStatus(LeadStatus.CONTACTED);
        LeadEntity second = new LeadEntity("Contacted Two", "contacted-2-" + UUID.randomUUID() + "@example.test", null);
        second.changeStatus(LeadStatus.CONTACTED);
        leadRepository.saveAndFlush(first);
        leadRepository.saveAndFlush(second);

        mockMvc.perform(get("/api/leads")
                        .param("status", "CONTACTED")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("CONTACTED"));
    }

    @ParameterizedTest
    @EnumSource(LeadStatus.class)
    @Transactional
    void filtersByEverySupportedStatus(LeadStatus statusFilter) throws Exception {
        LeadEntity lead = new LeadEntity(
                "Status Filter Lead",
                "status-filter-" + UUID.randomUUID() + "@example.test",
                null);
        lead.changeStatus(statusFilter);
        leadRepository.saveAndFlush(lead);

        mockMvc.perform(get("/api/leads")
                        .param("status", statusFilter.name())
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value(statusFilter.name()));
    }

    @Test
    @Transactional
    void ordersMatchingLeadsByCreatedAtThenDescendingUuid() throws Exception {
        UUID lowerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID higherId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        OffsetDateTime createdAt = OffsetDateTime.parse("2025-01-01T00:00:00Z");
        insertLead(lowerId, "order-lower@example.test", LeadStatus.CLOSED, createdAt);
        insertLead(higherId, "order-higher@example.test", LeadStatus.CLOSED, createdAt);

        mockMvc.perform(get("/api/leads").param("status", "CLOSED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(higherId.toString()))
                .andExpect(jsonPath("$.content[1].id").value(lowerId.toString()));
    }

    @Test
    void rejectsOutOfRangePageAndSize() throws Exception {
        mockMvc.perform(get("/api/leads").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request parameters are invalid."));

        mockMvc.perform(get("/api/leads").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsUnknownStatusFilterWithSafeBadRequest() throws Exception {
        mockMvc.perform(get("/api/leads").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Invalid request parameter."));
    }

    @Test
    void deletesLeadAndReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        insertLead(id, "delete-" + id + "@example.test", LeadStatus.NEW,
                OffsetDateTime.parse("2020-01-02T03:04:05Z"));

        MvcResult deleted = mockMvc.perform(delete("/api/leads/{id}", id))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(deleted.getResponse().getContentAsString()).isEmpty();
        assertThat(leadRepository.findById(id)).isEmpty();
        mockMvc.perform(get("/api/leads/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsNotFoundWhenDeletingMissingLead() throws Exception {
        mockMvc.perform(delete("/api/leads/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Lead not found."));
    }

    @Test
    void returnsSafeProblemDetailsWhenAnUnexpectedFailureOccurs() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new IllegalStateException("sensitive internal detail"))
                .when(leadRepository).findById(id);

        mockMvc.perform(get("/api/leads/{id}", id))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(
                        containsString("sensitive internal detail"))));
    }

    @Test
    void keepsUnmappedRoutesAsSafeNotFoundResponses() throws Exception {
        mockMvc.perform(get("/api/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void rejectsBlankNameWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest("  ", "valid@example.test", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed."));
    }

    @Test
    void rejectsMalformedEmailWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest("Ada Lovelace", "not-an-email", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsMalformedJsonWithSafeBadRequest() throws Exception {
        mockMvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail")
                        .value("Request body is malformed or contains invalid values."));
    }

    @Test
    void rejectsDuplicateNormalizedEmailWithConflict() throws Exception {
        String email = "duplicate-" + UUID.randomUUID() + "@example.test";

        mockMvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest("First Lead", email.toUpperCase(), null)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest("Second Lead", "  " + email + "  ", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("A lead with this email already exists."));
    }

    private String createRequest(String name, String email, String phone) {
        return """
                {
                  "name": "%s",
                  "email": "%s",
                  "phone": %s
                }
                """.formatted(name, email, phone == null ? "null" : "\"" + phone + "\"");
    }

    private void insertLead(UUID id, String email, LeadStatus status, OffsetDateTime createdAt) {
        jdbcTemplate.update(
                "INSERT INTO leads (id, name, email, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
                id,
                "Sorted Lead",
                email,
                status.name(),
                createdAt,
                createdAt);
    }

    private void saveLeadInStatus(LeadStatus status) {
        LeadEntity lead = new LeadEntity(
                "Statistics Lead",
                "stats-" + UUID.randomUUID() + "@example.test",
                null);
        lead.changeStatus(status);
        leadRepository.saveAndFlush(lead);
    }
}
