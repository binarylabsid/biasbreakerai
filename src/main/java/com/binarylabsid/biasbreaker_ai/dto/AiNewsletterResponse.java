package com.binarylabsid.biasbreaker_ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiNewsletterResponse {
    private String daily_insights;
    private List<AiStock> stocks;
    private List<String> strategy_tactics;
    private List<AiGlossary> glossary;

    @JsonSetter("strategy_tactics")
    public void parseStrategyTactics(JsonNode node) {
        List<String> tactics = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                if (item.isObject()) {
                    String tactic = item.has("tactic") ? item.get("tactic").asText() : "";
                    String reason = item.has("reason") ? item.get("reason").asText() : "";
                    tactics.add(tactic + " " + reason);
                } else if (item.isTextual()) {
                    tactics.add(item.asText());
                }
            }
        }
        this.strategy_tactics = tactics;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiStock {
        private String symbol;
        private String bull_read;
        private String bear_read;
        private String verdict;
        private String verdict_reason;
        private String beginner_insight_pe;
        private String beginner_insight_risk;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiGlossary {
        private String term;
        private String definition;
    }
}