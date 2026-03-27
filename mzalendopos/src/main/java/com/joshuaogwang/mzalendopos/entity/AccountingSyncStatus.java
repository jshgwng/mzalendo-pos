package com.joshuaogwang.mzalendopos.entity;

public enum AccountingSyncStatus {
    /** Queued but not yet sent */
    PENDING,
    /** Successfully synced; external reference stored */
    SYNCED,
    /** Provider returned a non-recoverable error */
    FAILED,
    /** Temporary failure; will be retried */
    RETRY,
    /** Accounting integration disabled; skipped */
    SKIPPED
}
