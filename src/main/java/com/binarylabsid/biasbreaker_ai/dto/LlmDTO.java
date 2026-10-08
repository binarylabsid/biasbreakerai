package com.binarylabsid.biasbreaker_ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

public class LlmDTO {

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Request {
        private String model;
        private List<Message> messages;
        private double temperature;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private String role;
        private String content;

        // TAMBAHKAN DUA FIELD INI AGAR JACKSON TIDAK ERROR
        private String refusal;
        private String reasoning;

        public Message() {
        }

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        private List<Choice> choices;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        private Message message;
    }
}