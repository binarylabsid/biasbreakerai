package com.binarylabsid.biasbreaker_ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompanyProfileDTO {
    private Overview overview;
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Overview {
        private String sub_sector;
    }
}
