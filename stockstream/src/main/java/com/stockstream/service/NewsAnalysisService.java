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

@Service
public class NewsAnalysisService {

    @Value("${ollama.api.url}")
    private String ollamaUrl;

    @Value("${ollama.model}")
    private String ollamaModel;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public NewsAnalysis analyzeNews(String newsText) {
        try {
            String prompt = """
                    You are an expert Indian stock market analyst with deep knowledge of:
                    - Sector analysis and supply chain mapping
                    - Macroeconomics and market sentiment behavior
                    - NSE/BSE listed companies and their business models
                    - Historical market patterns and reactions

                    Analyze this news completely:
                    "%s"

                    Return ONLY a valid JSON object. No explanation. No extra text outside JSON:

                    {
                      "summary": "2-3 line simple explanation of the news",
                      "sentiment": "POSITIVE",
                      "sentimentStrength": "STRONG",
                      "sentimentReason": "One line reason for sentiment",
                      "primarySector": "Main sector affected",
                      "eventType": "Type of market event",
                      "entities": ["Company1", "Company2"],
                      "keywords": ["keyword1", "keyword2", "keyword3"],
                      "primaryStocks": [
                        {
                          "stockName": "Company Name",
                          "stockCode": "TICKER",
                          "impact": "POSITIVE",
                          "reason": "Direct impact reason in one line",
                          "confidence": "HIGH",
                          "score": 85,
                          "layer": "DIRECT"
                        }
                      ],
                      "supplyChainStocks": [
                        {
                          "stockName": "Company Name",
                          "stockCode": "TICKER",
                          "impact": "POSITIVE",
                          "reason": "Supply chain impact reason",
                          "confidence": "MEDIUM",
                          "score": 65,
                          "layer": "SUPPLIER"
                        }
                      ],
                      "sectorImpacts": [
                        {
                          "sector": "Infrastructure",
                          "impact": "POSITIVE",
                          "reason": "Brief one line reason"
                        }
                      ],
                      "riskFactors": ["Risk factor 1", "Risk factor 2"],
                      "historicalPattern": "Brief comparison with similar past event and market reaction",
                      "shortTermOutlook": "BULLISH",
                      "mediumTermOutlook": "BULLISH",
                      "confidenceScore": 75,
                      "marketStrategy": "One actionable insight for traders"
                    }

                    Rules:
                    - sentiment must be exactly POSITIVE or NEGATIVE or NEUTRAL
                    - sentimentStrength must be exactly STRONG or MODERATE or WEAK
                    - impact values must be exactly POSITIVE or NEGATIVE or NEUTRAL
                    - shortTermOutlook and mediumTermOutlook must be BULLISH or BEARISH or NEUTRAL
                    - layer for primaryStocks must be DIRECT
                    - layer for supplyChainStocks must be SUPPLIER or LOGISTICS or BANKING or EPC
                    - Only include NSE/BSE listed Indian companies
                    - Maximum 5 primary stocks and 5 supply chain stocks
                    - confidenceScore must be a number between 0 and 100
                    - Return ONLY the JSON object, nothing else before or after
                    """.formatted(newsText);

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
            String aiResponse = jsonNode.get("response").asText().trim();

            // Extract clean JSON from response
            int start = aiResponse.indexOf("{");
            int end = aiResponse.lastIndexOf("}") + 1;
            if (start != -1 && end > start) {
                aiResponse = aiResponse.substring(start, end);
            }

            // Parse root JSON
            JsonNode root = objectMapper.readTree(aiResponse);

            NewsAnalysis analysis = new NewsAnalysis();
            analysis.setSummary(root.path("summary").asText(""));
            analysis.setSentiment(root.path("sentiment").asText("NEUTRAL"));
            analysis.setSentimentStrength(root.path("sentimentStrength").asText("MODERATE"));
            analysis.setSentimentReason(root.path("sentimentReason").asText(""));
            analysis.setPrimarySector(root.path("primarySector").asText(""));
            analysis.setEventType(root.path("eventType").asText(""));
            analysis.setHistoricalPattern(root.path("historicalPattern").asText(""));
            analysis.setShortTermOutlook(root.path("shortTermOutlook").asText("NEUTRAL"));
            analysis.setMediumTermOutlook(root.path("mediumTermOutlook").asText("NEUTRAL"));
            analysis.setConfidenceScore(root.path("confidenceScore").asInt(50));
            analysis.setMarketStrategy(root.path("marketStrategy").asText(""));

            // Parse string arrays
            analysis.setEntities(parseStringList(root.get("entities")));
            analysis.setKeywords(parseStringList(root.get("keywords")));
            analysis.setRiskFactors(parseStringList(root.get("riskFactors")));

            // Parse primary stocks using existing StockImpact model
            analysis.setPrimaryStocks(parseStockList(root.get("primaryStocks")));

            // Parse supply chain stocks using existing StockImpact model
            analysis.setSupplyChainStocks(parseStockList(root.get("supplyChainStocks")));

            // Parse sector impacts
            analysis.setSectorImpacts(parseSectorImpacts(root.get("sectorImpacts")));

            System.out.println("Analysis complete — sentiment: " + analysis.getSentiment()
                    + " confidence: " + analysis.getConfidenceScore());

            return analysis;

        } catch (Exception e) {
            System.out.println("News analysis error: " + e.getMessage());
            return getErrorResponse();
        }
    }

    // ─── Parse String List ────────────────────────
    private List<String> parseStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node == null || !node.isArray())
            return list;
        for (JsonNode item : node) {
            list.add(item.asText());
        }
        return list;
    }

    // ─── Parse Stock List using existing StockImpact ─
    // Reuses StockImpactService.StockImpact — no duplication
    private List<StockImpactService.StockImpact> parseStockList(JsonNode arrayNode) {
        List<StockImpactService.StockImpact> list = new ArrayList<>();
        if (arrayNode == null || !arrayNode.isArray())
            return list;
        for (JsonNode node : arrayNode) {
            try {
                StockImpactService.StockImpact s = new StockImpactService.StockImpact();
                s.setStockName(node.path("stockName").asText(""));
                s.setStockCode(node.path("stockCode").asText(""));
                s.setImpact(node.path("impact").asText("NEUTRAL"));
                s.setReason(node.path("reason").asText(""));
                s.setConfidence(node.path("confidence").asText("MEDIUM"));
                s.setScore(node.path("score").asInt(50));
                list.add(s);
            } catch (Exception e) {
                System.out.println("Skipping stock: " + e.getMessage());
            }
        }
        return list;
    }

    // ─── Parse Sector Impacts ─────────────────────
    private List<SectorImpact> parseSectorImpacts(JsonNode arrayNode) {
        List<SectorImpact> list = new ArrayList<>();
        if (arrayNode == null || !arrayNode.isArray())
            return list;
        for (JsonNode node : arrayNode) {
            try {
                SectorImpact s = new SectorImpact();
                s.setSector(node.path("sector").asText(""));
                s.setImpact(node.path("impact").asText("NEUTRAL"));
                s.setReason(node.path("reason").asText(""));
                list.add(s);
            } catch (Exception e) {
                System.out.println("Skipping sector: " + e.getMessage());
            }
        }
        return list;
    }

    // ─── Error Response ───────────────────────────
    private NewsAnalysis getErrorResponse() {
        NewsAnalysis r = new NewsAnalysis();
        r.setSummary("Analysis failed. Please try again with a clearer news headline.");
        r.setSentiment("NEUTRAL");
        r.setSentimentStrength("WEAK");
        r.setSentimentReason("Unable to analyze");
        r.setShortTermOutlook("NEUTRAL");
        r.setMediumTermOutlook("NEUTRAL");
        r.setConfidenceScore(0);
        r.setMarketStrategy("Retry with more specific news");
        r.setPrimaryStocks(new ArrayList<>());
        r.setSupplyChainStocks(new ArrayList<>());
        r.setSectorImpacts(new ArrayList<>());
        r.setEntities(new ArrayList<>());
        r.setKeywords(new ArrayList<>());
        r.setRiskFactors(new ArrayList<>());
        return r;
    }

    // ─── News Analysis Result Model ───────────────
    public static class NewsAnalysis {
        private String summary;
        private String sentiment;
        private String sentimentStrength;
        private String sentimentReason;
        private String primarySector;
        private String eventType;
        private List<String> entities;
        private List<String> keywords;
        // Reuses StockImpactService.StockImpact — no code duplication
        private List<StockImpactService.StockImpact> primaryStocks;
        private List<StockImpactService.StockImpact> supplyChainStocks;
        private List<SectorImpact> sectorImpacts;
        private List<String> riskFactors;
        private String historicalPattern;
        private String shortTermOutlook;
        private String mediumTermOutlook;
        private int confidenceScore;
        private String marketStrategy;

        public String getSummary() {
            return summary;
        }

        public void setSummary(String v) {
            this.summary = v;
        }

        public String getSentiment() {
            return sentiment;
        }

        public void setSentiment(String v) {
            this.sentiment = v;
        }

        public String getSentimentStrength() {
            return sentimentStrength;
        }

        public void setSentimentStrength(String v) {
            this.sentimentStrength = v;
        }

        public String getSentimentReason() {
            return sentimentReason;
        }

        public void setSentimentReason(String v) {
            this.sentimentReason = v;
        }

        public String getPrimarySector() {
            return primarySector;
        }

        public void setPrimarySector(String v) {
            this.primarySector = v;
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String v) {
            this.eventType = v;
        }

        public List<String> getEntities() {
            return entities;
        }

        public void setEntities(List<String> v) {
            this.entities = v;
        }

        public List<String> getKeywords() {
            return keywords;
        }

        public void setKeywords(List<String> v) {
            this.keywords = v;
        }

        public List<StockImpactService.StockImpact> getPrimaryStocks() {
            return primaryStocks;
        }

        public void setPrimaryStocks(List<StockImpactService.StockImpact> v) {
            this.primaryStocks = v;
        }

        public List<StockImpactService.StockImpact> getSupplyChainStocks() {
            return supplyChainStocks;
        }

        public void setSupplyChainStocks(List<StockImpactService.StockImpact> v) {
            this.supplyChainStocks = v;
        }

        public List<SectorImpact> getSectorImpacts() {
            return sectorImpacts;
        }

        public void setSectorImpacts(List<SectorImpact> v) {
            this.sectorImpacts = v;
        }

        public List<String> getRiskFactors() {
            return riskFactors;
        }

        public void setRiskFactors(List<String> v) {
            this.riskFactors = v;
        }

        public String getHistoricalPattern() {
            return historicalPattern;
        }

        public void setHistoricalPattern(String v) {
            this.historicalPattern = v;
        }

        public String getShortTermOutlook() {
            return shortTermOutlook;
        }

        public void setShortTermOutlook(String v) {
            this.shortTermOutlook = v;
        }

        public String getMediumTermOutlook() {
            return mediumTermOutlook;
        }

        public void setMediumTermOutlook(String v) {
            this.mediumTermOutlook = v;
        }

        public int getConfidenceScore() {
            return confidenceScore;
        }

        public void setConfidenceScore(int v) {
            this.confidenceScore = v;
        }

        public String getMarketStrategy() {
            return marketStrategy;
        }

        public void setMarketStrategy(String v) {
            this.marketStrategy = v;
        }
    }

    // ─── Sector Impact Model ──────────────────────
    public static class SectorImpact {
        private String sector;
        private String impact;
        private String reason;

        public String getSector() {
            return sector;
        }

        public void setSector(String v) {
            this.sector = v;
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
    }
}