package com.joshuaogwang.mzalendopos.entity;

public enum EfrisSubmissionStatus {
    /** Queued but not yet sent (offline or deferred) */
    PENDING,
    /** Successfully submitted; FRN received */
    SUBMITTED,
    /** URA returned an error code */
    FAILED,
    /** Timed out; will be retried */
    RETRY
}
