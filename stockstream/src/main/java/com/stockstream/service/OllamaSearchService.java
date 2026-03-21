package com.stockstream.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockstream.model.NewsArticle;
import com.stockstream.repository.NewsArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Service
public class OllamaSearchService {

    @Autowired
    private NewsArticleRepository newsArticleRepository;

    @Value("${ollama.api.url}")
    private String ollamaUrl;

    @Value("${ollama.model}")
    private String ollamaModel;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ─── Main Search Method ──────────────────────
    public List<NewsArticle> search(String userQuery) {
        System.out.println("User searched: " + userQuery);

        // Step 1: Extract keyword using Ollama AI
        String keyword = extractKeyword(userQuery);
        System.out.println("Extracted keyword: " + keyword);

        // Step 2: Search database with extracted keyword
        List<NewsArticle> results = newsArticleRepository.searchByKeyword(keyword);

        // Step 3: If no results found with AI keyword, use original query
        if (results.isEmpty()) {
            results = newsArticleRepository.searchByKeyword(userQuery);
        }

        System.out.println("Search results found: " + results.size());
        return results;
    }

    // ─── Extract Keyword Using Ollama ────────────
    private String extractKeyword(String userQuery) {
        try {
            String prompt = "Extract the main stock market or financial topic keyword from this search query. " +
                    "Return ONLY the keyword, nothing else. No explanation. " +
                    "Examples: 'latest news about gold' → 'gold', " +
                    "'what is happening with ITC stock' → 'ITC', " +
                    "'silver price today' → 'silver'. " +
                    "Query: '" + userQuery + "'";

            String requestBody = objectMapper.writeValueAsString(
                    new java.util.HashMap<>() {
                        {
                            put("model", ollamaModel);
                            put("prompt", prompt);
                            put("stream", false);
                        }
                    });

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            JsonNode jsonNode = objectMapper.readTree(response.body());
            String keyword = jsonNode.get("response").asText().trim();

            // Clean up keyword — remove quotes and extra spaces
            keyword = keyword.replaceAll("[\"']", "").trim();
            return keyword;

        } catch (Exception e) {
            System.out.println("Ollama error: " + e.getMessage() + " — using original query");
            return userQuery;
        }
    }
}