package com.stockstream.controller;

import com.stockstream.service.MarketDataService;
import com.stockstream.service.NewsAnalysisService;
import com.stockstream.service.OllamaSearchService;
import com.stockstream.service.RSSFeedService;
import com.stockstream.service.StockImpactService;
import com.stockstream.service.StockResearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = "*")
public class NewsController {

    @Autowired
    private RSSFeedService rssFeedService;
    @Autowired
    private OllamaSearchService ollamaSearchService;
    @Autowired
    private StockImpactService stockImpactService;
    @Autowired
    private MarketDataService marketDataService;
    @Autowired
    private NewsAnalysisService newsAnalysisService;
    @Autowired
    private StockResearchService stockResearchService;

    // ─── Breaking News — live from RSS ────────────
    @GetMapping("/breaking")
    public ResponseEntity<List<RSSFeedService.LiveArticle>> getBreakingNews() {
        return ResponseEntity.ok(rssFeedService.fetchAllLive());
    }

    // ─── Search — live RSS + AI keyword ──────────
    @GetMapping("/search")
    public ResponseEntity<List<RSSFeedService.LiveArticle>> searchNews(
            @RequestParam String q) {
        return ResponseEntity.ok(ollamaSearchService.search(q));
    }

    // ─── Category — live RSS filter ───────────────
    @GetMapping("/category/{category}")
    public ResponseEntity<List<RSSFeedService.LiveArticle>> getByCategory(
            @PathVariable String category) {
        return ResponseEntity.ok(rssFeedService.fetchByCategory(category.toUpperCase()));
    }

    // ─── Stock Impact Analysis ────────────────────
    @GetMapping("/impact")
    public ResponseEntity<StockImpactService.StockImpactResult> analyzeImpact(
            @RequestParam String q) {
        return ResponseEntity.ok(stockImpactService.analyzeNewsImpact(q));
    }

    // ─── Live Market Data ─────────────────────────
    @GetMapping("/market")
    public ResponseEntity<List<MarketDataService.MarketTicker>> getMarketData() {
        return ResponseEntity.ok(marketDataService.getTickerData());
    }

    // ─── Market Last Updated ──────────────────────
    @GetMapping("/market/lastupdated")
    public ResponseEntity<String> getLastUpdated() {
        return ResponseEntity.ok(marketDataService.getLastUpdated());
    }

    // ─── Market Refresh ───────────────────────────
    @PostMapping("/market/refresh")
    public ResponseEntity<String> refreshMarketData() {
        new Thread(() -> marketDataService.fetchMarketData()).start();
        return ResponseEntity.ok("Market refresh started");
    }

    // ─── Full 360 News Analysis ───────────────────
    @GetMapping("/analyze")
    public ResponseEntity<NewsAnalysisService.NewsAnalysis> analyzeNews(
            @RequestParam String q) {
        return ResponseEntity.ok(newsAnalysisService.analyzeNews(q));
    }

    // ─── Stock Deep Dive Research ─────────────────
    @GetMapping("/research")
    public ResponseEntity<StockResearchService.StockResearch> researchStock(
            @RequestParam String stock) {
        return ResponseEntity.ok(stockResearchService.researchStock(stock));
    }
}