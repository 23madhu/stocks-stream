package com.stockstream.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NewsScheduler {

    @Autowired
    private RSSFeedService rssFeedService;

    // ─── Fetch news every 15 minutes ────────────
    @Scheduled(fixedRate = 900000)
    public void fetchNewsAutomatically() {
        System.out.println("Scheduler triggered — fetching latest news...");
        rssFeedService.fetchAndSaveAllFeeds();
    }

    // ─── Fetch news immediately on startup ───────
    @Scheduled(initialDelay = 5000, fixedRate = 900000)
    public void fetchNewsOnStartup() {
        System.out.println("Initial news fetch on startup...");
        rssFeedService.fetchAndSaveAllFeeds();
    }
}