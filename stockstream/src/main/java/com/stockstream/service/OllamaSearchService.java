package com.stockstream.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OllamaSearchService {

    @Value("${ollama.api.url}")
    private String ollamaUrl;

    @Value("${ollama.model}")
    private String ollamaModel;

    @Autowired
    private RSSFeedService rssFeedService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public List<RSSFeedService.LiveArticle> search(String userQuery) {
        try {
            // Step 1: Extract keyword using Ollama
            String prompt = """
                    Extract the most important single search keyword from this query.
                    Return ONLY the keyword, nothing else.
                    Query: "%s"
                    """.formatted(userQuery);

            String requestBody = objectMapper.writeValueAsString(
                    new HashMap<String, Object>() {
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
                    request, HttpResponse.BodyHandlers.ofString());

            JsonNode jsonNode = objectMapper.readTree(response.body());
            String keyword = jsonNode.get("response").asText().trim()
                    .replace("\"", "").replace(".", "").toLowerCase();

            System.out.println("Search keyword extracted: " + keyword);

            // Step 2: Fetch live articles and filter by keyword
            List<RSSFeedService.LiveArticle> allArticles = rssFeedService.fetchAllLive();

            final String kw = keyword;
            List<RSSFeedService.LiveArticle> filtered = allArticles.stream()
                    .filter(a -> {
                        String title = a.getTitle() != null ? a.getTitle().toLowerCase() : "";
                        String desc = a.getDescription() != null ? a.getDescription().toLowerCase() : "";
                        return title.contains(kw) || desc.contains(kw);
                    })
                    .collect(Collectors.toList());

            // If no results with AI keyword, try original query words
            if (filtered.isEmpty()) {
                String[] words = userQuery.toLowerCase().split("\\s+");
                filtered = allArticles.stream()
                        .filter(a -> {
                            String title = a.getTitle() != null ? a.getTitle().toLowerCase() : "";
                            for (String word : words) {
                                if (word.length() > 3 && title.contains(word))
                                    return true;
                            }
                            return false;
                        })
                        .collect(Collectors.toList());
            }

            System.out.println("Search results: " + filtered.size() + " articles for: " + keyword);
            return filtered;

        } catch (Exception e) {
            System.out.println("Search error: " + e.getMessage());
            return rssFeedService.fetchAllLive();
        }
    }
}