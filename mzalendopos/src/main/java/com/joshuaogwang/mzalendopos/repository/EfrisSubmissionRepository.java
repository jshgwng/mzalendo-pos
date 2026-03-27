package com.joshuaogwang.mzalendopos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.joshuaogwang.mzalendopos.entity.EfrisSubmission;
import com.joshuaogwang.mzalendopos.entity.EfrisSubmissionStatus;

public interface EfrisSubmissionRepository extends JpaRepository<EfrisSubmission, Long> {

    Optional<EfrisSubmission> findBySaleId(Long saleId);

    List<EfrisSubmission> findByStatus(EfrisSubmissionStatus status);

    List<EfrisSubmission> findByStatusAndAttemptCountLessThan(EfrisSubmissionStatus status, int maxAttempts);
}
