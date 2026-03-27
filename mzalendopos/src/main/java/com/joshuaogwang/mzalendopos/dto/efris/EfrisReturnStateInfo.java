package com.joshuaogwang.mzalendopos.dto.efris;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EfrisReturnStateInfo {

    @JsonProperty("returnCode")
    private String returnCode;

    @JsonProperty("returnMessage")
    private String returnMessage;
}
