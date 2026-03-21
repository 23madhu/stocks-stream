package com.stockstream.controller;

import com.stockstream.model.NewsArticle;
import com.stockstream.service.OllamaSearchService;
import com.stockstream.service.RSSFeedService;
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

    // ─── Get Breaking News ───────────────────────
    // URL: GET http://localhost:8080/api/news/breaking
    @GetMapping("/breaking")
    public ResponseEntity<List<NewsArticle>> getBreakingNews() {
        List<NewsArticle> news = rssFeedService.getBreakingNews();
        return ResponseEntity.ok(news);
    }

    // ─── Search News with AI ─────────────────────
    // URL: GET http://localhost:8080/api/news/search?q=gold
    @GetMapping("/search")
    public ResponseEntity<List<NewsArticle>> searchNews(
            @RequestParam String q) {
        List<NewsArticle> results = ollamaSearchService.search(q);
        return ResponseEntity.ok(results);
    }

    // ─── Get News by Category ────────────────────
    // URL: GET http://localhost:8080/api/news/category/GOLD
    @GetMapping("/category/{category}")
    public ResponseEntity<List<NewsArticle>> getByCategory(
            @PathVariable String category) {
        List<NewsArticle> news = rssFeedService.getNewsByCategory(
                category.toUpperCase());
        return ResponseEntity.ok(news);
    }

    // ─── Manually Trigger News Fetch ─────────────
    // URL: POST http://localhost:8080/api/news/fetch
    @PostMapping("/fetch")
    public ResponseEntity<String> fetchNewsNow() {
        rssFeedService.fetchAndSaveAllFeeds();
        return ResponseEntity.ok("News fetch triggered successfully!");
    }
}