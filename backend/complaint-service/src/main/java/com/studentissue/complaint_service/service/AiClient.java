package com.studentissue.complaint_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.List;

@Component
public class AiClient {

    private final RestClient client;

    public AiClient(@Value("${ai.service-url}") String url,
                    @Value("${ai.timeout-ms}") int timeoutMs) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    /** Returnează categoria prezisă sau gol dacă AI service-ul nu răspunde. */
    public Optional<Map<String, Object>> classify(String text) {
        return post("/classify", Map.of("text", text));
    }

    public Optional<Map<String, Object>> priority(String text, int votes) {
        return post("/priority", Map.of("text", text, "votes", votes));
    }

    @SuppressWarnings("unchecked")
    private Optional<Map<String, Object>> post(String path, Object body) {
        try {
            return Optional.ofNullable(client.post().uri(path).body(body)
                    .retrieve().body(Map.class));
        } catch (Exception e) {
            return Optional.empty(); // fallback: AI oprit, plângerea se salvează oricum
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> duplicates(String text, Double lat, Double lon,
                                                List<Map<String, Object>> candidates) {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("latitude", lat);
        body.put("longitude", lon);
        body.put("candidates", candidates);
        return post("/duplicates", body)
                .map(r -> (List<Map<String, Object>>) r.get("duplicates"))
                .orElse(List.of());
    }
}