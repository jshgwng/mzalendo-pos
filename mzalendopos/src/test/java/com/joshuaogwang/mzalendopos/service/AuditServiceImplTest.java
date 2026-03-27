package com.joshuaogwang.mzalendopos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.joshuaogwang.mzalendopos.entity.Audit;
import com.joshuaogwang.mzalendopos.repository.AuditRepository;
import com.joshuaogwang.mzalendopos.service.serviceImpl.AuditServiceImpl;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private AuditRepository auditRepository;

    @InjectMocks
    private AuditServiceImpl auditService;

    private Audit audit;

    @BeforeEach
    void setUp() {
        audit = new Audit();
        audit.setId(1L);
        audit.setEntityName("User");
        audit.setEntityId("1");
        audit.setAction("CREATE");
        audit.setOldData("null");
        audit.setNewData("{\"username\":\"testuser\"}");
    }

    @Test
    void createAudit_setsTimestampAndSaves() {
        when(auditRepository.save(any(Audit.class))).thenReturn(audit);

        Audit created = auditService.createAudit(audit);

        verify(auditRepository).save(audit);
        assertThat(audit.getTimeStamp()).isNotNull();
        assertThat(created).isNotNull();
    }

    @Test
    void getAuditById_returnsAudit_whenFound() {
        when(auditRepository.findById(1L)).thenReturn(Optional.of(audit));

        Audit found = auditService.getAuditById(1L);

        assertThat(found.getEntityName()).isEqualTo("User");
    }

    @Test
    void getAuditById_throwsException_whenNotFound() {
        when(auditRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditService.getAuditById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getAllAudits_returnsPaginatedResults() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Audit> page = new PageImpl<>(List.of(audit), pageable, 1);
        when(auditRepository.findAll(pageable)).thenReturn(page);

        Page<Audit> result = auditService.getAllAudits(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void deleteAudit_deletesSuccessfully_whenExists() {
        when(auditRepository.existsById(1L)).thenReturn(true);

        auditService.deleteAudit(1L);

        verify(auditRepository).deleteById(1L);
    }

    @Test
    void deleteAudit_throwsException_whenNotFound() {
        when(auditRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> auditService.deleteAudit(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }
}
