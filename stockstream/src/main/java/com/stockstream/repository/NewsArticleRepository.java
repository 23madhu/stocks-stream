package com.stockstream.repository;

import com.stockstream.model.NewsArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    // Get latest 20 breaking news
    List<NewsArticle> findTop20ByOrderByPublishedAtDesc();

    // Get news by category
    List<NewsArticle> findByCategoryOrderByPublishedAtDesc(String category);

    // Search by keyword in title or description
    @Query("SELECT n FROM NewsArticle n WHERE " +
            "UPPER(n.title) LIKE UPPER(CONCAT('%', :keyword, '%')) OR " +
            "UPPER(n.description) LIKE UPPER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY n.publishedAt DESC")
    List<NewsArticle> searchByKeyword(@Param("keyword") String keyword);

    // Check if article already exists
    boolean existsByUrl(String url);

    // Get news by source
    List<NewsArticle> findBySourceOrderByPublishedAtDesc(String source);
}