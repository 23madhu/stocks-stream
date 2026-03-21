package com.stockstream.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "NEWS_ARTICLES")
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "news_seq")
    @SequenceGenerator(name = "news_seq", sequenceName = "NEWS_ARTICLES_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TITLE", length = 1000)
    private String title;

    @Column(name = "DESCRIPTION", length = 4000)
    private String description;

    @Column(name = "URL", length = 2000, unique = true)
    private String url;

    @Column(name = "IMAGE_URL", length = 2000)
    private String imageUrl;

    @Column(name = "SOURCE", length = 200)
    private String source;

    @Column(name = "CATEGORY", length = 100)
    private String category;

    @Column(name = "PUBLISHED_AT")
    private LocalDateTime publishedAt;

    @Column(name = "FETCHED_AT")
    private LocalDateTime fetchedAt;

    // ─── Constructors ───────────────────────────
    public NewsArticle() {
    }

    public NewsArticle(String title, String description, String url,
            String imageUrl, String source, String category,
            LocalDateTime publishedAt) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.imageUrl = imageUrl;
        this.source = source;
        this.category = category;
        this.publishedAt = publishedAt;
        this.fetchedAt = LocalDateTime.now();
    }

    // ─── Getters ────────────────────────────────
    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getSource() {
        return source;
    }

    public String getCategory() {
        return category;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }

    // ─── Setters ────────────────────────────────
    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void setFetchedAt(LocalDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    @Override
    public String toString() {
        return "NewsArticle{id=" + id + ", title=" + title + ", source=" + source + "}";
    }
}