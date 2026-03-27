package com.joshuaogwang.mzalendopos.service;

import java.util.List;

import com.joshuaogwang.mzalendopos.entity.EfrisSubmission;
import com.joshuaogwang.mzalendopos.entity.Sale;

public interface EfrisService {

    /**
     * Submit a completed sale to URA EFRIS.
     * If the submission succeeds the returned {@link EfrisSubmission} will have
     * status {@code SUBMITTED} and a populated {@code fiscalReceiptNumber}.
     * On network failure or URA error the status will be {@code PENDING} or
     * {@code FAILED} so the caller can surface the issue without blocking
     * the checkout flow.
     *
     * @param sale a COMPLETED sale with at least one item and a payment
     * @return the persisted submission record
     */
    EfrisSubmission submitInvoice(Sale sale);

    /**
     * Retry all PENDING/RETRY submissions that have not exceeded the
     * configured {@code efris.max-retry-attempts} limit.
     * Intended to be called by a scheduled job.
     *
     * @return list of submissions that were retried
     */
    List<EfrisSubmission> retryPendingSubmissions();

    /**
     * Return the most recent submission record for a given sale, or
     * {@code null} if EFRIS is disabled or no submission exists yet.
     */
    EfrisSubmission getSubmissionBySaleId(Long saleId);
}
