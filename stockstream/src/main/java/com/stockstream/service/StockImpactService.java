package com.stockstream.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockImpactService {

    @Value("${ollama.api.url}")
    private String ollamaUrl;

    @Value("${ollama.model}")
    private String ollamaModel;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public StockImpactResult analyzeNewsImpact(String newsQuery) {
        try {
            String prompt = """
                    You are a professional Indian stock market analyst with deep expertise in sector analysis, macroeconomics, and supply chain mapping.

                    Your task is to analyze the given news and identify ONLY genuinely impacted Indian stocks with strong logical or factual linkage.

                    NEWS:
                    "%s"

                    -----------------------------------
                    STEP 1: NEWS UNDERSTANDING
                    -----------------------------------
                    - Identify the PRIMARY sector (only one)
                    - Identify SUB-sectors (if any)
                    - Extract 5-10 KEYWORDS from the news

                    -----------------------------------
                    STEP 2: STRICT VALIDATION RULE (MANDATORY)
                    -----------------------------------
                    Include a company ONLY IF at least one condition is TRUE:

                    1. Company is directly mentioned in the news
                    2. Company has existing business/contracts in this exact domain
                    3. Company is a core player in this sector (execution/operator/manufacturer)
                    4. Company supplies critical materials/services essential to this sector
                    5. Company has historically benefited from similar events

                    If none of the above apply EXCLUDE the company
                    If unsure EXCLUDE (no guessing allowed)

                    -----------------------------------
                    STEP 3: 360 SUPPLY CHAIN ANALYSIS
                    -----------------------------------
                    Identify only RELEVANT layers:
                    - Core companies (direct beneficiaries)
                    - Execution companies (EPC / infra / manufacturers)
                    - Key suppliers (steel, cement, electricals, etc.)
                    - Supporting services (logistics, IT ONLY if strongly linked)
                    - Financial enablers (banks/NBFCs ONLY if clearly relevant)

                    Avoid weak, generic, or overextended connections.

                    -----------------------------------
                    STEP 4: CONFIDENCE SCORING (MANDATORY)
                    -----------------------------------
                    For EACH stock, calculate score out of 100:

                    Total Score =
                      DirectMention (0-40) +
                      SectorMatch (0-20) +
                      SupplyChainRole (0-20) +
                      HistoricalImpact (0-10) +
                      NewsStrength (0-10)

                    -----------------------------------
                    STEP 5: CONFIDENCE FILTERING
                    -----------------------------------
                    Score >= 80 = HIGH
                    Score >= 60 = MEDIUM
                    Score >= 40 = LOW
                    Score < 40 = EXCLUDE

                    -----------------------------------
                    STEP 6: OUTPUT SPLIT
                    -----------------------------------
                    1. actualImpact = Only HIGH confidence stocks (score >= 80)
                    2. predictedImpact = MEDIUM and LOW confidence stocks (score 40-79)

                    -----------------------------------
                    STEP 7: OUTPUT FORMAT (STRICT JSON ONLY)
                    -----------------------------------
                    {
                      "actualImpact": [
                        {
                          "stockName": "Company Name",
                          "stockCode": "TICKER",
                          "impact": "POSITIVE",
                          "reason": "Short fact-based reason",
                          "confidence": "HIGH",
                          "score": 85
                        }
                      ],
                      "predictedImpact": [
                        {
                          "stockName": "Company Name",
                          "stockCode": "TICKER",
                          "impact": "POSITIVE",
                          "reason": "Logical but not confirmed",
                          "confidence": "MEDIUM",
                          "score": 65
                        }
                      ]
                    }

                    STRICT RULES:
                    - Return ONLY valid JSON (no explanation, no extra text)
                    - Include only 5-12 stocks total
                    - Avoid unrelated large-cap companies
                    - No assumptions or guessing
                    - Prefer accuracy over quantity
                    - If no strong stocks found return empty arrays
                    """
                    .formatted(newsQuery);

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
                    request,
                    HttpResponse.BodyHandlers.ofString());

            JsonNode jsonNode = objectMapper.readTree(response.body());
            String aiResponse = jsonNode.get("response").asText().trim();

            // Extract JSON from response
            aiResponse = extractJson(aiResponse);

            JsonNode root = objectMapper.readTree(aiResponse);

            StockImpactResult result = new StockImpactResult();
            result.setActualImpact(parseStocks(root.get("actualImpact")));
            result.setPredictedImpact(parseStocks(root.get("predictedImpact")));

            return result;

        } catch (Exception e) {
            System.out.println("Stock impact error: " + e.getMessage());
            StockImpactResult empty = new StockImpactResult();
            empty.setActualImpact(new ArrayList<>());
            empty.setPredictedImpact(new ArrayList<>());
            return empty;
        }
    }

    private List<StockImpact> parseStocks(JsonNode arrayNode) {
        List<StockImpact> list = new ArrayList<>();
        if (arrayNode == null || !arrayNode.isArray())
            return list;
        for (JsonNode node : arrayNode) {
            try {
                StockImpact s = new StockImpact();
                s.setStockName(node.get("stockName").asText());
                s.setStockCode(node.get("stockCode").asText());
                s.setImpact(node.get("impact").asText());
                s.setReason(node.get("reason").asText());
                s.setConfidence(node.get("confidence").asText());
                s.setScore(node.get("score").asInt());
                list.add(s);
            } catch (Exception e) {
                System.out.println("Skipping stock: " + e.getMessage());
            }
        }
        return list;
    }

    private String extractJson(String text) {
        try {
            int start = text.indexOf("{");
            int end = text.lastIndexOf("}") + 1;
            if (start != -1 && end > start) {
                return text.substring(start, end);
            }
        } catch (Exception e) {
            System.out.println("JSON extract error: " + e.getMessage());
        }
        return "{\"actualImpact\": [], \"predictedImpact\": []}";
    }

    // ─── Result wrapper ──────────────────────────
    public static class StockImpactResult {
        private List<StockImpact> actualImpact;
        private List<StockImpact> predictedImpact;

        public List<StockImpact> getActualImpact() {
            return actualImpact;
        }

        public void setActualImpact(List<StockImpact> actualImpact) {
            this.actualImpact = actualImpact;
        }

        public List<StockImpact> getPredictedImpact() {
            return predictedImpact;
        }

        public void setPredictedImpact(List<StockImpact> predictedImpact) {
            this.predictedImpact = predictedImpact;
        }
    }

    // ─── Stock Impact model ──────────────────────
    public static class StockImpact {
        private String stockName;
        private String stockCode;
        private String impact;
        private String reason;
        private String confidence;
        private int score;

        public String getStockName() {
            return stockName;
        }

        public void setStockName(String v) {
            this.stockName = v;
        }

        public String getStockCode() {
            return stockCode;
        }

        public void setStockCode(String v) {
            this.stockCode = v;
        }

        public String getImpact() {
            return impact;
        }

        public void setImpact(String v) {
            this.impact = v;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String v) {
            this.reason = v;
        }

        public String getConfidence() {
            return confidence;
        }

        public void setConfidence(String v) {
            this.confidence = v;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int v) {
            this.score = v;
        }
    }
}