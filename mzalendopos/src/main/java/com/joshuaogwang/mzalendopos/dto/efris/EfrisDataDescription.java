package com.joshuaogwang.mzalendopos.dto.efris;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EfrisDataDescription {

    /** 0 = plain text, 1 = encrypted */
    @JsonProperty("codeType")
    private String codeType;

    /** 1 = AES encrypted */
    @JsonProperty("encryptCode")
    private String encryptCode;

    /** 0 = no zip */
    @JsonProperty("zipCode")
    private String zipCode;
}
