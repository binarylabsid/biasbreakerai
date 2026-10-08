package com.binarylabsid.biasbreaker_ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScannerDTO {
    private DailyChange top_losers;
    private DailyChange top_gainers;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DailyChange {
        @JsonProperty("1d")
        private List<CompanyChange> oneDay;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CompanyChange {
        private String symbol;
        private String name;
        private Double price_change;
    }
}
