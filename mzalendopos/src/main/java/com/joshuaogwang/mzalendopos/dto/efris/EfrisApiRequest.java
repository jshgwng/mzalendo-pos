package com.joshuaogwang.mzalendopos.dto.efris;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Top-level envelope for all URA EFRIS API calls.
 */
@Data
@Builder
public class EfrisApiRequest {

    @JsonProperty("data")
    private EfrisRequestData data;

    @JsonProperty("globalInfo")
    private EfrisGlobalInfo globalInfo;

    @JsonProperty("returnStateInfo")
    private EfrisReturnStateInfo returnStateInfo;
}
