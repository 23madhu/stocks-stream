package com.stockstream.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.stockstream.model.NewsArticle;
import com.stockstream.repository.NewsArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.net.HttpURLConnection;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class RSSFeedService {

    @Autowired
    private NewsArticleRepository newsArticleRepository;

    // ─── All RSS Feed URLs ───────────────────────
    private final List<String[]> RSS_FEEDS = List.of(
            // {url, category}
            // ─── STOCKS (Indian) ────────────────────────
            new String[] { "https://economictimes.indiatimes.com/markets/rss.cms", "STOCKS" },
            new String[] { "https://feeds.feedburner.com/ndtvprofit-latest", "STOCKS" },
            new String[] { "https://www.business-standard.com/rss/markets-106.rss", "STOCKS" },

            // ─── GLOBAL ─────────────────────────────────
            new String[] { "https://feeds.finance.yahoo.com/rss/2.0/headline", "GLOBAL" },
            new String[] { "https://feeds.reuters.com/reuters/businessNews", "GLOBAL" },
            new String[] { "https://www.cnbc.com/id/100003114/device/rss/rss.html", "GLOBAL" },

            // ─── COMMODITIES ────────────────────────────
            new String[] { "https://www.kitco.com/rss/news.rss", "COMMODITIES" },
            new String[] { "https://oilprice.com/rss/main", "COMMODITIES" },
            new String[] { "https://feeds.reuters.com/reuters/commoditiesNews", "COMMODITIES" },

            // ─── CRYPTO ─────────────────────────────────
            new String[] { "https://cointelegraph.com/rss", "CRYPTO" },
            new String[] { "https://www.coindesk.com/arc/outboundfeeds/rss/", "CRYPTO" },

            // ─── TECH ───────────────────────────────────
            new String[] { "https://techcrunch.com/feed/", "TECH" },

            // ─── GENERAL ────────────────────────────────
            new String[] { "http://feeds.bbci.co.uk/news/business/rss.xml", "GENERAL" },
            new String[] { "https://timesofindia.indiatimes.com/rssfeeds/-2128936835.cms", "GENERAL" });

    // ─── Fetch All Feeds ─────────────────────────
    public void fetchAndSaveAllFeeds() {
        System.out.println("Starting RSS feed fetch...");
        int totalSaved = 0;

        for (String[] feed : RSS_FEEDS) {
            String feedUrl = feed[0];
            String category = feed[1];

            try {
                List<NewsArticle> articles = fetchSingleFeed(feedUrl, category);
                for (NewsArticle article : articles) {
                    // Skip if already exists in database
                    if (!newsArticleRepository.existsByUrl(article.getUrl())) {
                        newsArticleRepository.save(article);
                        totalSaved++;
                    }
                }
                System.out.println("Fetched from: " + feedUrl + " → " + articles.size() + " articles");
            } catch (Exception e) {
                System.out.println("Failed to fetch: " + feedUrl + " → " + e.getMessage());
            }
        }
        System.out.println("Total new articles saved: " + totalSaved);
    }

    private List<NewsArticle> fetchSingleFeed(String feedUrl, String category) {
        List<NewsArticle> articles = new ArrayList<>();
        try {
            URL url = new URL(feedUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            SyndFeedInput input = new SyndFeedInput();
            input.setAllowDoctypes(true); // Fix DOCTYPE error for ET and other feeds
            SyndFeed feed = input.build(new XmlReader(connection.getInputStream()));

            for (SyndEntry entry : feed.getEntries()) {
                try {
                    NewsArticle article = new NewsArticle();
                    article.setTitle(cleanText(entry.getTitle()));
                    article.setDescription(getDescription(entry));
                    article.setUrl(cleanUrl(entry.getLink()));
                    article.setSource(feed.getTitle());
                    article.setCategory(category);
                    article.setFetchedAt(LocalDateTime.now());

                    if (entry.getPublishedDate() != null) {
                        article.setPublishedAt(entry.getPublishedDate()
                                .toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime());
                    } else {
                        article.setPublishedAt(LocalDateTime.now());
                    }

                    if (article.getUrl() != null && !article.getUrl().isEmpty()) {
                        articles.add(article);
                    }
                } catch (Exception e) {
                    System.out.println("Skipping entry: " + e.getMessage());
                }
            }
            System.out.println("Fetched from: " + feedUrl + " → " + articles.size() + " articles");
        } catch (Exception e) {
            System.out.println("Failed to fetch: " + feedUrl + " → " + e.getMessage());
        }
        return articles;
    }

    // ─── Helper: Get Description ─────────────────
    private String getDescription(SyndEntry entry) {
        try {
            if (entry.getDescription() != null) {
                String desc = entry.getDescription().getValue();
                // Remove HTML tags
                desc = desc.replaceAll("<[^>]*>", "");
                // Limit to 500 characters
                if (desc.length() > 500) {
                    desc = desc.substring(0, 500) + "...";
                }
                return desc;
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }

    // ─── Helper: Clean Text ──────────────────────
    private String cleanText(String text) {
        if (text == null)
            return "";
        return text.replaceAll("<[^>]*>", "").trim();
    }

    private String cleanUrl(String url) {
        if (url == null)
            return null;
        try {
            if (url.contains("?")) {
                url = url.substring(0, url.indexOf("?"));
            }
            if (url.contains("#")) {
                url = url.substring(0, url.indexOf("#"));
            }
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            return url.trim();
        } catch (Exception e) {
            return url;
        }
    }

    // ─── Get Breaking News ───────────────────────
    public List<NewsArticle> getBreakingNews() {
        return newsArticleRepository.findTop20ByOrderByPublishedAtDesc();
    }

    // ─── Get News by Category ────────────────────
    public List<NewsArticle> getNewsByCategory(String category) {
        return newsArticleRepository.findByCategoryOrderByPublishedAtDesc(category);
    }
}