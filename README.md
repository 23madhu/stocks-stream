# stocks-stream
Project Overview
StockStream is a full stack AI-powered stock news aggregator built using Spring Boot and React. It automatically collects real-time financial news from multiple sources across the internet, stores them in an Oracle database, and displays them in a clean card-based website. It also uses local AI to understand natural language searches and analyze which stocks are affected by any news event.

Problem It Solves
Every day thousands of news articles are published across hundreds of websites. A stock market investor has to manually visit multiple websites like Economic Times, Moneycontrol, Reuters, Yahoo Finance, CNBC, Kitco and many more just to stay updated. This takes a lot of time and effort. StockStream solves this by automatically collecting all news in one place and presenting it in a simple, clean interface. It also helps investors understand which stocks will be impacted by any news — something that normally requires expert financial knowledge.

Key Features
1. Automatic News Collection
The application automatically fetches news from 15+ RSS feeds every 30 minutes without any manual intervention. Sources include Economic Times, NDTV Profit, Business Standard, Reuters, CNBC, Yahoo Finance, Kitco, CoinTelegraph, TechCrunch and more. Each feed is categorized as Stocks, Global, Commodities, Crypto, Tech or General.
2. Duplicate Prevention
When the same news article appears across multiple RSS feeds, the system automatically detects and skips duplicates using URL-based comparison. This is enforced at both the Java application level and the Oracle database level through a UNIQUE constraint on the URL column.
3. Natural Language Search
Users can search using plain English sentences like "latest news about gold" or "what is happening with ITC stock". The search query is sent to Ollama AI running locally on the machine. The Llama 3.2 model extracts the key topic from the query and searches the Oracle database for matching articles.
4. Stock Impact Analyzer
This is the most powerful feature. Users can type any news event and the system will tell them which Indian stocks will be affected, whether the impact is positive or negative, and give a brief reason for each stock. For example if a user types "budget announced 7 new fastest trains from various states" the system will identify stocks like IRCTC, Larsen and Toubro, RVNL, Titagarh Rail Systems and explain why each is affected.
5. Category Filtering
News is organized into categories — Stocks, Global, Commodities, Crypto, Tech and General. Users can filter news by clicking category buttons on the website.
6. Clean Card-Based UI
News articles are displayed as beautiful cards showing the title, description, source, category badge, publish date and a Read button to open the full article.

Technology Stack
Backend:
Java 25 and Spring Boot 3.3.5 form the backend of the application. Spring Boot handles REST API creation, database connection, scheduling and dependency injection. Maven is used as the build tool.
Database:
Oracle Database 21c Express Edition is used to store all news articles. The schema includes a NEWS_ARTICLES table with columns for title, description, URL, source, category, published date and fetch date. Oracle sequences are used for auto-generating primary keys. A UNIQUE constraint on the URL column prevents duplicate entries.
AI and NLP:
Ollama runs locally on the developer's machine and hosts the Llama 3.2 language model. This eliminates any cloud API costs and keeps all data processing local. The AI is used for two purposes — extracting keywords from search queries and analyzing stock market impact of news events.
News Sources:
The ROME library is used to parse RSS feeds. RSS is a standard format used by news websites to publish their articles. No API keys are needed for RSS feeds making the entire news collection system completely free.
Frontend:
React with Vite is used for the frontend. Axios handles HTTP communication between the React frontend and the Spring Boot backend. The UI is built with plain CSS styles for maximum compatibility.
Caching:
Redis is included in the stack for response caching to improve API response times for frequently accessed data.
Version Control:
Git and GitHub are used for source code management.

System Architecture
News Websites (ET, Reuters, CNBC, Kitco etc)
           ↓ RSS Feeds
    Spring Scheduler (every 30 min)
           ↓
    RSSFeedService (ROME parser)
           ↓
    Duplicate Check (existsByUrl)
           ↓
    Oracle Database (NEWS_ARTICLES table)
           ↓
    REST API (Spring Boot controllers)
           ↓
    React Frontend (localhost:5173)
           ↑
    User Search Query
           ↓
    OllamaSearchService
           ↓
    Llama 3.2 AI (localhost:11434)
           ↓
    Keyword Extraction
           ↓
    Oracle Database Query
           ↓
    Search Results to Frontend

API Endpoints
The application exposes four REST API endpoints:
GET /api/news/breaking returns the latest 20 news articles ordered by publish date.
GET /api/news/search?q=gold accepts a natural language query, processes it through Ollama AI to extract keywords, and returns matching articles from Oracle.
GET /api/news/category/STOCKS returns all articles belonging to a specific category such as STOCKS, GLOBAL, COMMODITIES, CRYPTO, TECH or GENERAL.
GET /api/news/impact?q=budget announced new trains accepts any news description and returns a list of affected Indian stocks with their impact direction and brief reason.
POST /api/news/fetch manually triggers an immediate news fetch from all RSS feeds.

Database Design
sqlTABLE: NEWS_ARTICLES
─────────────────────────────────────────
ID           NUMBER        PRIMARY KEY
TITLE        VARCHAR2(1000)
DESCRIPTION  VARCHAR2(4000)
URL          VARCHAR2(2000) UNIQUE
IMAGE_URL    VARCHAR2(2000)
SOURCE       VARCHAR2(200)
CATEGORY     VARCHAR2(100)
PUBLISHED_AT TIMESTAMP
FETCHED_AT   TIMESTAMP      DEFAULT SYSTIMESTAMP

SEQUENCE: NEWS_ARTICLES_SEQ
INDEX: IDX_NEWS_CATEGORY on CATEGORY
INDEX: IDX_NEWS_PUBLISHED on PUBLISHED_AT
```

---

## What Makes This Project Unique

Most news aggregator projects use paid APIs like NewsAPI or OpenAI which have strict rate limits and monthly costs. StockStream uses completely free and open source technologies. RSS feeds provide unlimited news without any API key. Ollama runs AI locally without any cloud dependency or cost. Oracle XE is free for development use. This means the entire project runs at zero cost forever.

The Stock Impact Analyzer is a particularly unique feature that combines financial domain knowledge with AI to provide actionable insights for investors. This kind of feature is typically found only in expensive professional financial platforms.

---

## Future Enhancements

The project can be extended to include real-time stock price display using free BSE/NSE data, sentiment analysis to classify each news article as positive or negative, email or WhatsApp alerts when important news about a user's watchlist stocks appears, a portfolio tracker where users can add their stocks and see relevant news, and deployment to a cloud platform to make it accessible from anywhere.

---

## Project Statistics
```
Backend files     : 7 Java files
Frontend files    : 1 React component
RSS feed sources  : 15+ websites
News categories   : 7 (Stocks, Global, Commodities,
                       Crypto, Tech, General, Market)
Database tables   : 1 (NEWS_ARTICLES)
API endpoints     : 5
AI models used    : Llama 3.2 via Ollama
Total cost        : Zero rupees

This project demonstrates practical knowledge of full stack Java development, REST API design, database integration with Oracle, AI integration, frontend development with React, and real-world RSS data processing — making it an excellent portfolio project for a career in software development.
