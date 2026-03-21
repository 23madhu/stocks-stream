package com.stockstream.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.stockstream.model.NewsArticle;
import com.stockstream.repository.NewsArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
            new String[] { "https://economictimes.indiatimes.com/markets/rss.cms", "STOCKS" },
            new String[] { "https://www.moneycontrol.com/rss/results.xml", "STOCKS" },
            new String[] { "https://feeds.feedburner.com/ndtvprofit-latest", "STOCKS" },
            new String[] { "https://www.business-standard.com/rss/markets-106.rss", "STOCKS" },
            new String[] { "https://feeds.finance.yahoo.com/rss/2.0/headline", "GLOBAL" },
            new String[] { "https://feeds.reuters.com/reuters/businessNews", "GLOBAL" },
            new String[] { "https://www.kitco.com/rss/news.rss", "GOLD" },
            new String[] { "https://goldprice.org/feed", "GOLD" });

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

    // ─── Fetch Single Feed ───────────────────────
    private List<NewsArticle> fetchSingleFeed(String feedUrl, String category) {
        List<NewsArticle> articles = new ArrayList<>();

        try {
            URL url = new URL(feedUrl);
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(url));

            for (SyndEntry entry : feed.getEntries()) {
                try {
                    NewsArticle article = new NewsArticle();
                    article.setTitle(cleanText(entry.getTitle()));
                    article.setDescription(getDescription(entry));
                    article.setUrl(entry.getLink());
                    article.setSource(feed.getTitle());
                    article.setCategory(category);
                    article.setFetchedAt(LocalDateTime.now());

                    // Set published date
                    if (entry.getPublishedDate() != null) {
                        article.setPublishedAt(entry.getPublishedDate()
                                .toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime());
                    } else {
                        article.setPublishedAt(LocalDateTime.now());
                    }

                    // Only add if URL is not null
                    if (article.getUrl() != null && !article.getUrl().isEmpty()) {
                        articles.add(article);
                    }

                } catch (Exception e) {
                    System.out.println("Skipping entry: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("Error reading feed: " + feedUrl + " → " + e.getMessage());
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

    // ─── Get Breaking News ───────────────────────
    public List<NewsArticle> getBreakingNews() {
        return newsArticleRepository.findTop20ByOrderByPublishedAtDesc();
    }

    // ─── Get News by Category ────────────────────
    public List<NewsArticle> getNewsByCategory(String category) {
        return newsArticleRepository.findByCategoryOrderByPublishedAtDesc(category);
    }
}