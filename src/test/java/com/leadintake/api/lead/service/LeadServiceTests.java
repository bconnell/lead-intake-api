package com.leadintake.api.lead.service;

import com.leadintake.api.lead.dto.LeadStatsResponse;
import com.leadintake.api.lead.model.LeadStatus;
import com.leadintake.api.lead.repository.LeadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void aggregatesStatisticsFromDatabaseCountQueries() {
        when(leadRepository.count()).thenReturn(7L);
        when(leadRepository.countByStatus(LeadStatus.NEW)).thenReturn(3L);
        when(leadRepository.countByStatus(LeadStatus.CONTACTED)).thenReturn(1L);
        when(leadRepository.countByStatus(LeadStatus.QUALIFIED)).thenReturn(2L);
        when(leadRepository.countByStatus(LeadStatus.CLOSED)).thenReturn(1L);
        when(leadRepository.countByStatus(LeadStatus.REJECTED)).thenReturn(0L);

        LeadStatsResponse statistics = leadService.stats();

        assertThat(statistics).isEqualTo(new LeadStatsResponse(7, 3, 1, 2, 1, 0));
        verify(leadRepository).count();
        verify(leadRepository).countByStatus(LeadStatus.NEW);
        verify(leadRepository).countByStatus(LeadStatus.CONTACTED);
        verify(leadRepository).countByStatus(LeadStatus.QUALIFIED);
        verify(leadRepository).countByStatus(LeadStatus.CLOSED);
        verify(leadRepository).countByStatus(LeadStatus.REJECTED);
        verifyNoMoreInteractions(leadRepository);
    }
}
