package com.joshuaogwang.mzalendopos.dto.accounting;

import com.joshuaogwang.mzalendopos.entity.AccountingSyncStatus;
import lombok.Builder;
import lombok.Data;

/**
 * Returned by each provider adapter after a sync attempt.
 */
@Data
@Builder
public class AccountingSyncResult {

    private AccountingSyncStatus status;

    /** The ID/number assigned by the accounting tool (e.g. QuickBooks invoice ID) */
    private String externalReference;

    /** Human-readable URL to view the record in the accounting tool */
    private String externalUrl;

    /** Error detail when status is FAILED or RETRY */
    private String errorMessage;
}
