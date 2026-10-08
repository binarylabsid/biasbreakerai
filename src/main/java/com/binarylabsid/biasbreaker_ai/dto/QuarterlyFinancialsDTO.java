package com.binarylabsid.biasbreaker_ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuarterlyFinancialsDTO {
    private String symbol;
    private String date;
    private Long revenue;
    private Long earnings;
}
