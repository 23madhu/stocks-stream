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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class StockResearchService {

    @Value("${ollama.api.url}")
    private String ollamaUrl;

    @Value("${ollama.model}")
    private String ollamaModel;

    @Autowired
    private NewsArticleRepository newsArticleRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public StockResearch researchStock(String stockName) {
        try {
            // Fetch recent news from Oracle DB
            List<NewsArticle> recentNews = newsArticleRepository
                    .searchByKeyword(stockName);

            // Build news context for AI
            StringBuilder newsContext = new StringBuilder();
            int count = 0;
            for (NewsArticle article : recentNews) {
                if (count >= 5)
                    break;
                newsContext.append("- ").append(article.getTitle()).append("\n");
                count++;
            }

            String prompt = """
                    You are a professional Indian stock market research analyst.
                    Provide a complete 360-degree research report for this stock.

                    STOCK: "%s"

                    RECENT NEWS FROM DATABASE:
                    %s

                    Return ONLY valid JSON in exactly this format. No extra text:

                    {
                      "stockName": "Full company name",
                      "stockCode": "NSE ticker symbol",
                      "sector": "Sector name",
                      "industry": "Industry name",
                      "description": "2-3 line company description",

                      "fundamentals": {
                        "marketCap": "Large Cap / Mid Cap / Small Cap",
                        "peRatio": "Estimated P/E ratio as string",
                        "pbRatio": "Estimated P/B ratio as string",
                        "debtToEquity": "Debt to equity ratio",
                        "roe": "Return on equity percentage",
                        "roce": "Return on capital employed",
                        "dividendYield": "Dividend yield percentage",
                        "revenueGrowth": "Revenue growth YoY",
                        "profitGrowth": "Profit growth YoY",
                        "fundamentalRating": "STRONG or GOOD or AVERAGE or WEAK"
                      },

                      "technical": {
                        "trend": "UPTREND or DOWNTREND or SIDEWAYS",
                        "rsi": "RSI value estimated",
                        "macd": "BULLISH CROSSOVER or BEARISH CROSSOVER or NEUTRAL",
                        "movingAverage": "ABOVE 200 DMA or BELOW 200 DMA",
                        "support": "Key support level estimated",
                        "resistance": "Key resistance level estimated",
                        "volumeTrend": "INCREASING or DECREASING or STABLE",
                        "technicalSignal": "BUY or SELL or HOLD or NEUTRAL"
                      },

                      "strengths": ["Strength 1", "Strength 2", "Strength 3"],
                      "weaknesses": ["Weakness 1", "Weakness 2"],
                      "opportunities": ["Opportunity 1", "Opportunity 2"],
                      "threats": ["Threat 1", "Threat 2"],

                      "upcomingEvents": [
                        {
                          "event": "Q4 Results",
                          "expectedDate": "April 2026",
                          "impact": "HIGH or MEDIUM or LOW",
                          "description": "Brief description"
                        }
                      ],

                      "analystView": {
                        "rating": "STRONG BUY or BUY or HOLD or SELL or STRONG SELL",
                        "targetPrice": "Estimated target price",
                        "upside": "Estimated upside percentage",
                        "timeframe": "6-12 months",
                        "summary": "2-3 line analyst summary"
                      },

                      "riskLevel": "HIGH or MEDIUM or LOW",
                      "investorType": "Long Term or Short Term or Both",
                      "overallScore": 75
                    }

                    Rules:
                    - Use real knowledge about this Indian company
                    - All values as strings except overallScore which is a number
                    - overallScore is between 0 and 100
                    - Return ONLY the JSON object
                    """.formatted(stockName, newsContext.toString());

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

            int start = aiResponse.indexOf("{");
            int end = aiResponse.lastIndexOf("}") + 1;
            if (start != -1 && end > start) {
                aiResponse = aiResponse.substring(start, end);
            }

            StockResearch research = objectMapper.readValue(aiResponse, StockResearch.class);

            // Add recent news from DB
            List<NewsItem> newsItems = new ArrayList<>();
            for (NewsArticle article : recentNews) {
                if (newsItems.size() >= 8)
                    break;
                NewsItem item = new NewsItem();
                item.setTitle(article.getTitle());
                item.setSource(article.getSource());
                item.setUrl(article.getUrl());
                item.setPublishedAt(article.getPublishedAt() != null
                        ? article.getPublishedAt().toString()
                        : "");
                newsItems.add(item);
            }
            research.setRecentNews(newsItems);

            System.out.println("Stock research complete for: " + stockName);
            return research;

        } catch (Exception e) {
            System.out.println("Stock research error: " + e.getMessage());
            return getErrorResponse(stockName);
        }
    }

    private StockResearch getErrorResponse(String stockName) {
        StockResearch r = new StockResearch();
        r.setStockName(stockName);
        r.setStockCode("N/A");
        r.setSector("Unknown");
        r.setDescription("Unable to fetch research. Please try again.");
        r.setOverallScore(0);
        r.setRecentNews(new ArrayList<>());
        r.setUpcomingEvents(new ArrayList<>());
        r.setStrengths(new ArrayList<>());
        r.setWeaknesses(new ArrayList<>());
        r.setOpportunities(new ArrayList<>());
        r.setThreats(new ArrayList<>());
        return r;
    }

    // ─── Models ───────────────────────────────────

    public static class StockResearch {
        private String stockName, stockCode, sector, industry, description;
        private Fundamentals fundamentals;
        private Technical technical;
        private List<String> strengths, weaknesses, opportunities, threats;
        private List<UpcomingEvent> upcomingEvents;
        private List<NewsItem> recentNews;
        private AnalystView analystView;
        private String riskLevel, investorType;
        private int overallScore;

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

        public String getSector() {
            return sector;
        }

        public void setSector(String v) {
            this.sector = v;
        }

        public String getIndustry() {
            return industry;
        }

        public void setIndustry(String v) {
            this.industry = v;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String v) {
            this.description = v;
        }

        public Fundamentals getFundamentals() {
            return fundamentals;
        }

        public void setFundamentals(Fundamentals v) {
            this.fundamentals = v;
        }

        public Technical getTechnical() {
            return technical;
        }

        public void setTechnical(Technical v) {
            this.technical = v;
        }

        public List<String> getStrengths() {
            return strengths;
        }

        public void setStrengths(List<String> v) {
            this.strengths = v;
        }

        public List<String> getWeaknesses() {
            return weaknesses;
        }

        public void setWeaknesses(List<String> v) {
            this.weaknesses = v;
        }

        public List<String> getOpportunities() {
            return opportunities;
        }

        public void setOpportunities(List<String> v) {
            this.opportunities = v;
        }

        public List<String> getThreats() {
            return threats;
        }

        public void setThreats(List<String> v) {
            this.threats = v;
        }

        public List<UpcomingEvent> getUpcomingEvents() {
            return upcomingEvents;
        }

        public void setUpcomingEvents(List<UpcomingEvent> v) {
            this.upcomingEvents = v;
        }

        public List<NewsItem> getRecentNews() {
            return recentNews;
        }

        public void setRecentNews(List<NewsItem> v) {
            this.recentNews = v;
        }

        public AnalystView getAnalystView() {
            return analystView;
        }

        public void setAnalystView(AnalystView v) {
            this.analystView = v;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(String v) {
            this.riskLevel = v;
        }

        public String getInvestorType() {
            return investorType;
        }

        public void setInvestorType(String v) {
            this.investorType = v;
        }

        public int getOverallScore() {
            return overallScore;
        }

        public void setOverallScore(int v) {
            this.overallScore = v;
        }
    }

    public static class Fundamentals {
        private String marketCap, peRatio, pbRatio, debtToEquity;
        private String roe, roce, dividendYield, revenueGrowth;
        private String profitGrowth, fundamentalRating;

        public String getMarketCap() {
            return marketCap;
        }

        public void setMarketCap(String v) {
            this.marketCap = v;
        }

        public String getPeRatio() {
            return peRatio;
        }

        public void setPeRatio(String v) {
            this.peRatio = v;
        }

        public String getPbRatio() {
            return pbRatio;
        }

        public void setPbRatio(String v) {
            this.pbRatio = v;
        }

        public String getDebtToEquity() {
            return debtToEquity;
        }

        public void setDebtToEquity(String v) {
            this.debtToEquity = v;
        }

        public String getRoe() {
            return roe;
        }

        public void setRoe(String v) {
            this.roe = v;
        }

        public String getRoce() {
            return roce;
        }

        public void setRoce(String v) {
            this.roce = v;
        }

        public String getDividendYield() {
            return dividendYield;
        }

        public void setDividendYield(String v) {
            this.dividendYield = v;
        }

        public String getRevenueGrowth() {
            return revenueGrowth;
        }

        public void setRevenueGrowth(String v) {
            this.revenueGrowth = v;
        }

        public String getProfitGrowth() {
            return profitGrowth;
        }

        public void setProfitGrowth(String v) {
            this.profitGrowth = v;
        }

        public String getFundamentalRating() {
            return fundamentalRating;
        }

        public void setFundamentalRating(String v) {
            this.fundamentalRating = v;
        }
    }

    public static class Technical {
        private String trend, rsi, macd, movingAverage;
        private String support, resistance, volumeTrend, technicalSignal;

        public String getTrend() {
            return trend;
        }

        public void setTrend(String v) {
            this.trend = v;
        }

        public String getRsi() {
            return rsi;
        }

        public void setRsi(String v) {
            this.rsi = v;
        }

        public String getMacd() {
            return macd;
        }

        public void setMacd(String v) {
            this.macd = v;
        }

        public String getMovingAverage() {
            return movingAverage;
        }

        public void setMovingAverage(String v) {
            this.movingAverage = v;
        }

        public String getSupport() {
            return support;
        }

        public void setSupport(String v) {
            this.support = v;
        }

        public String getResistance() {
            return resistance;
        }

        public void setResistance(String v) {
            this.resistance = v;
        }

        public String getVolumeTrend() {
            return volumeTrend;
        }

        public void setVolumeTrend(String v) {
            this.volumeTrend = v;
        }

        public String getTechnicalSignal() {
            return technicalSignal;
        }

        public void setTechnicalSignal(String v) {
            this.technicalSignal = v;
        }
    }

    public static class UpcomingEvent {
        private String event, expectedDate, impact, description;

        public String getEvent() {
            return event;
        }

        public void setEvent(String v) {
            this.event = v;
        }

        public String getExpectedDate() {
            return expectedDate;
        }

        public void setExpectedDate(String v) {
            this.expectedDate = v;
        }

        public String getImpact() {
            return impact;
        }

        public void setImpact(String v) {
            this.impact = v;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String v) {
            this.description = v;
        }
    }

    public static class AnalystView {
        private String rating, targetPrice, upside, timeframe, summary;

        public String getRating() {
            return rating;
        }

        public void setRating(String v) {
            this.rating = v;
        }

        public String getTargetPrice() {
            return targetPrice;
        }

        public void setTargetPrice(String v) {
            this.targetPrice = v;
        }

        public String getUpside() {
            return upside;
        }

        public void setUpside(String v) {
            this.upside = v;
        }

        public String getTimeframe() {
            return timeframe;
        }

        public void setTimeframe(String v) {
            this.timeframe = v;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String v) {
            this.summary = v;
        }
    }

    public static class NewsItem {
        private String title, source, url, publishedAt;

        public String getTitle() {
            return title;
        }

        public void setTitle(String v) {
            this.title = v;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String v) {
            this.source = v;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String v) {
            this.url = v;
        }

        public String getPublishedAt() {
            return publishedAt;
        }

        public void setPublishedAt(String v) {
            this.publishedAt = v;
        }
    }
}