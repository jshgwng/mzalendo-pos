package com.joshuaogwang.mzalendopos.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.joshuaogwang.mzalendopos.entity.EfrisSubmission;
import com.joshuaogwang.mzalendopos.service.EfrisService;

@RestController
@RequestMapping("/api/v1/efris")
public class EfrisController {

    @Autowired
    private EfrisService efrisService;

    /** Get the EFRIS submission record for a specific sale. */
    @GetMapping("/submission/sale/{saleId}")
    public ResponseEntity<EfrisSubmission> getSubmissionBySale(@PathVariable Long saleId) {
        EfrisSubmission submission = efrisService.getSubmissionBySaleId(saleId);
        if (submission == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(submission);
    }

    /**
     * Manually trigger a retry of all pending/failed EFRIS submissions.
     * Useful when connectivity to URA is restored after an outage.
     */
    @PostMapping("/retry")
    public ResponseEntity<List<EfrisSubmission>> retryPending() {
        return ResponseEntity.ok(efrisService.retryPendingSubmissions());
    }
}
