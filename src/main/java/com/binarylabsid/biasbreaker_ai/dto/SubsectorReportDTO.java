package com.binarylabsid.biasbreaker_ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubsectorReportDTO {

    private Statistics statistics;
    private Stability stability;
    private Growth growth;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Statistics {
        @JsonProperty("filtered_median_pe")
        private Double filtered_median_pe;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Stability {
        @JsonProperty("weighted_max_drawdown")
        private Double weighted_max_drawdown;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Growth {
        @JsonProperty("weighted_avg_growth_data")
        private Map<String, GrowthData> weightedAvgGrowthData;

        @JsonProperty("growth_forecasts")
        private Map<String, ForecastData> growthForecasts;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GrowthData {
        private Double avg_annual_revenue_growth;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ForecastData {
        @JsonProperty("eps_growth")
        private Double epsGrowth;
        @JsonProperty("revenue_growth")
        private Double revenueGrowth;
    }
}