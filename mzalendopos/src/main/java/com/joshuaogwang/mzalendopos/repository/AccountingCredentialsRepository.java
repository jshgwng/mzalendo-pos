package com.joshuaogwang.mzalendopos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.joshuaogwang.mzalendopos.entity.AccountingCredentials;
import com.joshuaogwang.mzalendopos.entity.AccountingProviderType;

public interface AccountingCredentialsRepository extends JpaRepository<AccountingCredentials, Long> {

    Optional<AccountingCredentials> findByProvider(AccountingProviderType provider);

    boolean existsByProviderAndActiveTrue(AccountingProviderType provider);
}
