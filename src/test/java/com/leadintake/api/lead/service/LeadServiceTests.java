package com.leadintake.api.lead.service;

import com.leadintake.api.lead.dto.LeadStatsResponse;
import com.leadintake.api.lead.model.LeadStatus;
import com.leadintake.api.lead.repository.LeadRepository;
import com.leadintake.api.lead.repository.LeadStatusCount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadServiceTests {

    @Mock
    private LeadRepository leadRepository;

    @InjectMocks
    private LeadService leadService;

    @Test
    void aggregatesAllStatisticsFromOneGroupedDatabaseResult() {
        when(leadRepository.countByStatusGrouped()).thenReturn(List.of(
                new LeadStatusCount(LeadStatus.CLOSED, 1L),
                new LeadStatusCount(LeadStatus.QUALIFIED, 2L),
                new LeadStatusCount(LeadStatus.NEW, 3L),
                new LeadStatusCount(LeadStatus.CONTACTED, 1L)));

        LeadStatsResponse statistics = leadService.stats();

        assertThat(statistics).isEqualTo(new LeadStatsResponse(7, 3, 1, 2, 1, 0));
        verify(leadRepository).countByStatusGrouped();
        verifyNoMoreInteractions(leadRepository);
    }

    @Test
    void returnsZerosForAnEmptyGroupedResult() {
        when(leadRepository.countByStatusGrouped()).thenReturn(List.of());

        assertThat(leadService.stats()).isEqualTo(new LeadStatsResponse(0, 0, 0, 0, 0, 0));
        verify(leadRepository).countByStatusGrouped();
        verifyNoMoreInteractions(leadRepository);
    }
}
