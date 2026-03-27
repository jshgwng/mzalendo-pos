package com.joshuaogwang.mzalendopos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.joshuaogwang.mzalendopos.entity.AccountingProviderType;
import com.joshuaogwang.mzalendopos.entity.AccountingSync;
import com.joshuaogwang.mzalendopos.entity.AccountingSyncStatus;

public interface AccountingSyncRepository extends JpaRepository<AccountingSync, Long> {

    Optional<AccountingSync> findByProviderAndPosReference(
            AccountingProviderType provider, String posReference);

    List<AccountingSync> findByStatusAndAttemptCountLessThan(
            AccountingSyncStatus status, int maxAttempts);

    Page<AccountingSync> findByProvider(AccountingProviderType provider, Pageable pageable);

    Page<AccountingSync> findByStatus(AccountingSyncStatus status, Pageable pageable);

    List<AccountingSync> findByPosReference(String posReference);
}
