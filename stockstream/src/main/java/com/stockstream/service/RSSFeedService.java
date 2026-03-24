package com.stockstream.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RSSFeedService {

    // ─── All RSS Feed Sources ─────────────────────
    private final String[][] RSS_FEEDS = {
            // Indian Stock Market
            { "https://economictimes.indiatimes.com/markets/rss.cms", "STOCKS" },
            { "https://www.moneycontrol.com/rss/results.xml", "STOCKS" },
            { "https://www.business-standard.com/rss/markets-106.rss", "STOCKS" },
            { "https://feeds.feedburner.com/ndtvprofit-latest", "STOCKS" },
            // Global Markets
            { "https://feeds.finance.yahoo.com/rss/2.0/headline?s=^GSPC&region=US&lang=en-US", "GLOBAL" },
            { "https://feeds.reuters.com/reuters/businessNews", "GLOBAL" },
            { "https://www.cnbc.com/id/100003114/device/rss/rss.html", "GLOBAL" },
            // Commodities
            { "https://www.kitco.com/rss/kitco-news-gold.rss", "COMMODITIES" },
            { "https://oilprice.com/rss/main", "COMMODITIES" },
            { "https://feeds.reuters.com/reuters/commoditiesNews", "COMMODITIES" },
            // Crypto
            { "https://cointelegraph.com/rss", "CRYPTO" },
            { "https://www.coindesk.com/arc/outboundfeeds/rss/", "CRYPTO" },
            // Tech
            { "https://techcrunch.com/feed/", "TECH" },
            // General Business
            { "https://timesofindia.indiatimes.com/rssfeeds/1898055.cms", "GENERAL" },
            { "https://feeds.bbci.co.uk/news/business/rss.xml", "GENERAL" },
    };

    // ─── Live Article Model (no DB entity) ───────
    public static class LiveArticle {
        private String title;
        private String description;
        private String url;
        private String source;
        private String category;
        private LocalDateTime publishedAt;

        public String getTitle() {
            return title;
        }

        public void setTitle(String v) {
            this.title = v;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String v) {
            this.description = v;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String v) {
            this.url = v;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String v) {
            this.source = v;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String v) {
            this.category = v;
        }

        public LocalDateTime getPublishedAt() {
            return publishedAt;
        }

        public void setPublishedAt(LocalDateTime v) {
            this.publishedAt = v;
        }
    }

    // ─── Fetch ALL feeds live ─────────────────────
    // Called every time frontend requests breaking news
    public List<LiveArticle> fetchAllLive() {
        List<LiveArticle> allArticles = new ArrayList<>();

        for (String[] feed : RSS_FEEDS) {
            String feedUrl = feed[0];
            String category = feed[1];
            try {
                List<LiveArticle> articles = fetchSingleFeed(feedUrl, category);
                allArticles.addAll(articles);
                System.out.println("Fetched " + articles.size() + " from: " + feedUrl);
            } catch (Exception e) {
                System.out.println("Failed: " + feedUrl + " → " + e.getMessage());
            }
        }

        // Sort by newest first
        allArticles.sort((a, b) -> {
            if (a.getPublishedAt() == null)
                return 1;
            if (b.getPublishedAt() == null)
                return -1;
            return b.getPublishedAt().compareTo(a.getPublishedAt());
        });

        // Remove duplicates by URL
        Set<String> seen = new HashSet<>();
        List<LiveArticle> unique = new ArrayList<>();
        for (LiveArticle a : allArticles) {
            if (a.getUrl() != null && seen.add(a.getUrl())) {
                unique.add(a);
            }
        }

        System.out.println("Total live articles: " + unique.size());
        return unique;
    }

    // ─── Fetch by category ────────────────────────
    public List<LiveArticle> fetchByCategory(String category) {
        List<LiveArticle> result = new ArrayList<>();
        for (String[] feed : RSS_FEEDS) {
            if (feed[1].equalsIgnoreCase(category)) {
                result.addAll(fetchSingleFeed(feed[0], feed[1]));
            }
        }
        result.sort((a, b) -> {
            if (a.getPublishedAt() == null)
                return 1;
            if (b.getPublishedAt() == null)
                return -1;
            return b.getPublishedAt().compareTo(a.getPublishedAt());
        });
        return result;
    }

    // ─── Fetch single RSS feed ────────────────────
    private List<LiveArticle> fetchSingleFeed(String feedUrl, String category) {
        List<LiveArticle> articles = new ArrayList<>();
        try {
            URL url = new URL(feedUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            SyndFeedInput input = new SyndFeedInput();
            input.setAllowDoctypes(true);
            SyndFeed feed = input.build(new XmlReader(connection.getInputStream()));

            for (SyndEntry entry : feed.getEntries()) {
                try {
                    LiveArticle article = new LiveArticle();
                    article.setTitle(cleanText(entry.getTitle()));
                    article.setDescription(getDescription(entry));
                    article.setUrl(cleanUrl(entry.getLink()));
                    article.setSource(feed.getTitle());
                    article.setCategory(category);

                    if (entry.getPublishedDate() != null) {
                        article.setPublishedAt(entry.getPublishedDate()
                                .toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime());
                    } else {
                        article.setPublishedAt(LocalDateTime.now());
                    }

                    if (article.getUrl() != null && !article.getUrl().isEmpty()
                            && article.getTitle() != null && !article.getTitle().isEmpty()) {
                        articles.add(article);
                    }
                } catch (Exception e) {
                    // skip bad entry
                }
            }
        } catch (Exception e) {
            System.out.println("Feed error [" + feedUrl + "]: " + e.getMessage());
        }
        return articles;
    }

    // ─── Helpers ──────────────────────────────────
    private String cleanText(String text) {
        if (text == null)
            return "";
        return text.replaceAll("<[^>]*>", "").trim();
    }

    private String getDescription(SyndEntry entry) {
        try {
            if (entry.getDescription() != null) {
                return cleanText(entry.getDescription().getValue());
            }
            if (entry.getContents() != null && !entry.getContents().isEmpty()) {
                return cleanText(entry.getContents().get(0).getValue());
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private String cleanUrl(String url) {
        if (url == null)
            return null;
        try {
            if (url.contains("?"))
                url = url.substring(0, url.indexOf("?"));
            if (url.contains("#"))
                url = url.substring(0, url.indexOf("#"));
            if (url.endsWith("/"))
                url = url.substring(0, url.length() - 1);
            return url.trim();
        } catch (Exception e) {
            return url;
        }
    }
}