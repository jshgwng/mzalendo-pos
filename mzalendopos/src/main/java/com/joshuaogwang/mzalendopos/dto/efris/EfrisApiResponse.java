package com.joshuaogwang.mzalendopos.dto.efris;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Top-level envelope returned by URA EFRIS API.
 * The {@code data.content} field is Base64-encoded AES-encrypted JSON
 * containing the fiscal receipt number and QR code.
 */
@Data
public class EfrisApiResponse {

    @JsonProperty("data")
    private EfrisResponseData data;

    @JsonProperty("globalInfo")
    private EfrisGlobalInfo globalInfo;

    @JsonProperty("returnStateInfo")
    private EfrisReturnStateInfo returnStateInfo;

    @Data
    public static class EfrisResponseData {

        /** Base64-encoded AES-encrypted response content */
        @JsonProperty("content")
        private String content;

        @JsonProperty("signature")
        private String signature;

        @JsonProperty("dataDescription")
        private EfrisDataDescription dataDescription;
    }
}
