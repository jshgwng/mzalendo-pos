package com.joshuaogwang.mzalendopos.dto.efris;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EfrisRequestData {

    /** Base64-encoded AES-encrypted invoice JSON */
    @JsonProperty("content")
    private String content;

    /** Digital signature (optional for sandbox) */
    @JsonProperty("signature")
    private String signature;

    @JsonProperty("dataDescription")
    private EfrisDataDescription dataDescription;
}
