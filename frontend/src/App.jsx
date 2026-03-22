import { useState, useEffect } from "react";
import axios from "axios";

const API = "http://localhost:8080/api/news";

const COLORS = {
  bg: "#0a0e1a",
  surface: "#111827",
  card: "#1a2235",
  border: "#1e2d45",
  accent: "#6366f1",
  accentHover: "#818cf8",
  green: "#10b981",
  red: "#ef4444",
  amber: "#f59e0b",
  gray: "#6b7280",
  text: "#f1f5f9",
  textMuted: "#94a3b8",
  textDim: "#64748b",
};

const CAT_COLORS = {
  STOCKS:      { bg: "#1e3a5f", text: "#60a5fa", dot: "#3b82f6" },
  GLOBAL:      { bg: "#1a3a2a", text: "#34d399", dot: "#10b981" },
  COMMODITIES: { bg: "#3a2a10", text: "#fbbf24", dot: "#f59e0b" },
  CRYPTO:      { bg: "#2d1a4a", text: "#a78bfa", dot: "#8b5cf6" },
  TECH:        { bg: "#1a2a3a", text: "#38bdf8", dot: "#0ea5e9" },
  GENERAL:     { bg: "#2a2a2a", text: "#9ca3af", dot: "#6b7280" },
  GOLD:        { bg: "#3a2e10", text: "#fcd34d", dot: "#fbbf24" },
  MARKET:      { bg: "#1e3a2a", text: "#6ee7b7", dot: "#34d399" },
};

const GlobalStyles = () => (
  <style>{`
    @keyframes pulse {
      0%, 100% { transform: scaleY(1); opacity: 0.4; }
      50% { transform: scaleY(2.5); opacity: 1; }
    }
    @keyframes shimmer {
      0% { background-position: -200% 0; }
      100% { background-position: 200% 0; }
    }
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(8px); }
      to { opacity: 1; transform: translateY(0); }
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
    @keyframes pulseDot {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.3; }
    }
  `}</style>
);

function AILoader() {
  return (
    <div style={{ textAlign: "center", padding: "32px 0" }}>
      <div style={{ position: "relative", width: "64px", height: "64px", margin: "0 auto 16px" }}>
        <div style={{
          width: "64px", height: "64px", borderRadius: "50%",
          border: `2px solid ${COLORS.border}`,
          borderTopColor: COLORS.accent,
          animation: "spin 1s linear infinite",
        }} />
        <div style={{
          position: "absolute", top: "50%", left: "50%",
          transform: "translate(-50%, -50%)",
          width: "8px", height: "8px", borderRadius: "50%",
          backgroundColor: COLORS.accent,
        }} />
      </div>
      <p style={{ color: COLORS.accent, fontSize: "14px", fontWeight: "500", margin: "0 0 4px" }}>
        AI is analyzing market impact
      </p>
      <p style={{ color: COLORS.textDim, fontSize: "12px", margin: 0 }}>
        Running 360° supply chain analysis...
      </p>
    </div>
  );
}

function SkeletonCard() {
  const shimmer = {
    background: `linear-gradient(90deg, ${COLORS.card} 25%, #1e2d45 50%, ${COLORS.card} 75%)`,
    backgroundSize: "200% 100%",
    animation: "shimmer 1.5s infinite",
    borderRadius: "6px",
  };
  return (
    <div style={{ background: COLORS.card, borderRadius: "12px", padding: "20px", border: `1px solid ${COLORS.border}` }}>
      <div style={{ ...shimmer, height: "20px", width: "60%", marginBottom: "12px" }} />
      <div style={{ ...shimmer, height: "14px", width: "100%", marginBottom: "8px" }} />
      <div style={{ ...shimmer, height: "14px", width: "80%", marginBottom: "16px" }} />
      <div style={{ display: "flex", justifyContent: "space-between" }}>
        <div style={{ ...shimmer, height: "12px", width: "30%" }} />
        <div style={{ ...shimmer, height: "12px", width: "20%" }} />
      </div>
    </div>
  );
}

function StockCard({ item, type }) {
  const impactColor =
    item.impact === "POSITIVE" ? COLORS.green :
    item.impact === "NEGATIVE" ? COLORS.red : COLORS.amber;
  const borderColor =
    type === "actual" ? impactColor :
    item.confidence === "MEDIUM" ? COLORS.amber : COLORS.gray;
  const scoreColor =
    item.score >= 80 ? COLORS.green :
    item.score >= 60 ? COLORS.amber : COLORS.gray;

  return (
    <div style={{
      background: COLORS.card, borderRadius: "12px", padding: "16px",
      border: `1px solid ${COLORS.border}`,
      borderTop: `3px solid ${borderColor}`,
      width: "200px", animation: "fadeIn 0.4s ease forwards",
    }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "8px" }}>
        <div>
          <div style={{ fontWeight: "700", fontSize: "14px", color: COLORS.text }}>{item.stockCode}</div>
          <div style={{ fontSize: "11px", color: COLORS.textDim, marginTop: "2px" }}>{item.stockName}</div>
        </div>
        <div style={{
          backgroundColor: item.impact === "POSITIVE" ? "#052e16" : item.impact === "NEGATIVE" ? "#450a0a" : "#431407",
          color: impactColor, padding: "3px 8px", borderRadius: "20px", fontSize: "10px", fontWeight: "600",
        }}>
          {item.impact === "POSITIVE" ? "▲" : item.impact === "NEGATIVE" ? "▼" : "►"} {item.impact}
        </div>
      </div>
      <p style={{ fontSize: "11px", color: COLORS.textMuted, lineHeight: "1.5", margin: "0 0 12px", minHeight: "44px" }}>
        {item.reason}
      </p>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <span style={{
          fontSize: "10px", color: type === "actual" ? COLORS.green : COLORS.amber,
          fontWeight: "600", backgroundColor: type === "actual" ? "#052e16" : "#431407",
          padding: "2px 8px", borderRadius: "20px",
        }}>
          {item.confidence}
        </span>
        <div style={{ display: "flex", alignItems: "center", gap: "4px" }}>
          <div style={{ width: "32px", height: "4px", borderRadius: "2px", backgroundColor: COLORS.border, overflow: "hidden" }}>
            <div style={{ height: "100%", width: `${item.score}%`, backgroundColor: scoreColor, borderRadius: "2px" }} />
          </div>
          <span style={{ fontSize: "10px", color: scoreColor, fontWeight: "600" }}>{item.score}</span>
        </div>
      </div>
    </div>
  );
}

export default function App() {

  // ── STATE VARIABLES ───────────────────────────
  const [news, setNews] = useState([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [category, setCategory] = useState("ALL");
  const [impactQuery, setImpactQuery] = useState("");
  const [impacts, setImpacts] = useState({ actualImpact: [], predictedImpact: [] });
  const [impactLoading, setImpactLoading] = useState(false);
  const [analyzed, setAnalyzed] = useState(false);
  const [ticker, setTicker] = useState([]);
  const [selectedMarket, setSelectedMarket] = useState(null);
  const [marketLoading, setMarketLoading] = useState(false);
  const [lastUpdated, setLastUpdated] = useState("");
  const [countdown, setCountdown] = useState(300);
  const [stockQuery, setStockQuery] = useState("");
  const [stockResearch, setStockResearch] = useState(null);
  const [stockLoading, setStockLoading] = useState(false);
  const [showResearch, setShowResearch] = useState(false);

  const cats = ["ALL", "STOCKS", "GLOBAL", "COMMODITIES", "CRYPTO", "TECH", "GENERAL"];

  // ── ON PAGE LOAD ──────────────────────────────
  useEffect(() => {
    loadBreaking();
    fetchMarketData();
    const marketInterval = setInterval(fetchMarketData, 300000);
    const countdownInterval = setInterval(() => {
      setCountdown((prev) => (prev <= 1 ? 300 : prev - 1));
    }, 1000);
    return () => {
      clearInterval(marketInterval);
      clearInterval(countdownInterval);
    };
  }, []);

  // ── API FUNCTIONS ─────────────────────────────
  async function loadBreaking() {
    setLoading(true);
    try {
      const res = await axios.get(API + "/breaking");
      setNews(res.data);
    } catch (e) { console.log("Breaking news error: " + e.message); }
    setLoading(false);
  }

  async function doSearch() {
    if (!query.trim()) return;
    setLoading(true);
    try {
      const res = await axios.get(API + "/search?q=" + encodeURIComponent(query));
      setNews(res.data);
    } catch (e) { console.log("Search error: " + e.message); }
    setLoading(false);
  }

  async function loadCategory(cat) {
    setCategory(cat);
    setLoading(true);
    try {
      const url = cat === "ALL" ? API + "/breaking" : API + "/category/" + cat;
      const res = await axios.get(url);
      setNews(res.data);
    } catch (e) { console.log("Category error: " + e.message); }
    setLoading(false);
  }

  async function analyzeImpact() {
    if (!impactQuery.trim()) return;
    setImpactLoading(true);
    setAnalyzed(false);
    setImpacts({ actualImpact: [], predictedImpact: [] });
    try {
      const res = await axios.get(API + "/impact?q=" + encodeURIComponent(impactQuery));
      setImpacts(res.data);
      setAnalyzed(true);
    } catch (e) { console.log("Impact error: " + e.message); }
    setImpactLoading(false);
  }

  async function fetchMarketData() {
    try {
      const res = await axios.get(API + "/market");
      setTicker(res.data);
      const timeRes = await axios.get(API + "/market/lastupdated");
      setLastUpdated(timeRes.data);
      setCountdown(300);
    } catch (e) { console.log("Market error: " + e.message); }
  }

  async function manualRefresh() {
    setMarketLoading(true);
    try {
      await axios.post(API + "/market/refresh");
      await new Promise((resolve) => setTimeout(resolve, 15000));
      await fetchMarketData();
    } catch (e) { console.log("Refresh error: " + e.message); }
    setMarketLoading(false);
  }

  async function searchStock() {
    if (!stockQuery.trim()) return;
    setStockLoading(true);
    setStockResearch(null);
    setShowResearch(true);
    try {
      const res = await axios.get(API + "/research?stock=" + encodeURIComponent(stockQuery));
      setStockResearch(res.data);
    } catch (e) { console.log("Stock research error: " + e.message); }
    setStockLoading(false);
  }

  // ── HELPERS ───────────────────────────────────
  function formatDate(d) {
    if (!d) return "";
    const date = new Date(d);
    const now = new Date();
    const diff = Math.floor((now - date) / 60000);
    if (diff < 60) return `${diff}m ago`;
    if (diff < 1440) return `${Math.floor(diff / 60)}h ago`;
    return date.toLocaleDateString("en-IN", { day: "numeric", month: "short" });
  }

  const catStyle = (cat) => CAT_COLORS[cat] || { bg: "#2a2a2a", text: "#9ca3af", dot: "#6b7280" };

  const typeBg = (type) =>
    type === "INDEX" ? "#1e3a5f" : type === "COMMODITY" ? "#3a2a10" : type === "CRYPTO" ? "#2d1a4a" : "#1a3a2a";

  const typeColor = (type) =>
    type === "INDEX" ? "#60a5fa" : type === "COMMODITY" ? "#fbbf24" : type === "CRYPTO" ? "#a78bfa" : "#34d399";

  // ── RENDER ────────────────────────────────────
  return (
    <div style={{ backgroundColor: COLORS.bg, minHeight: "100vh", fontFamily: "'Inter','Segoe UI',sans-serif", color: COLORS.text }}>

      <GlobalStyles />

      {/* ════════════════════════════════════════════
          MARKET DETAIL POPUP
          Opens when user clicks any ticker card
      ════════════════════════════════════════════ */}
      {selectedMarket && (
        <div
          onClick={() => setSelectedMarket(null)}
          style={{
            position: "fixed", top: 0, left: 0, right: 0, bottom: 0,
            backgroundColor: "rgba(0,0,0,0.75)",
            display: "flex", alignItems: "center", justifyContent: "center",
            zIndex: 9999, animation: "fadeIn 0.2s ease",
          }}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            style={{
              backgroundColor: "#0d1117",
              border: `1px solid ${COLORS.border}`,
              borderRadius: "16px", padding: "24px",
              width: "520px", maxWidth: "92vw",
              animation: "fadeIn 0.25s ease",
            }}
          >
            {/* Popup header */}
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "16px" }}>
              <div>
                <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "6px" }}>
                  <span style={{
                    fontSize: "10px", padding: "2px 8px", borderRadius: "4px",
                    fontWeight: "700", letterSpacing: "0.5px",
                    backgroundColor: typeBg(selectedMarket.type),
                    color: typeColor(selectedMarket.type),
                  }}>
                    {selectedMarket.type}
                  </span>
                  <span style={{ fontSize: "11px", color: COLORS.textDim }}>NSE · Yahoo Finance</span>
                </div>
                <h2 style={{ margin: "0 0 6px", fontSize: "22px", fontWeight: "800", color: COLORS.text }}>
                  {selectedMarket.symbol}
                </h2>
                <div style={{ display: "flex", alignItems: "baseline", gap: "10px" }}>
                  <span style={{ fontSize: "30px", fontWeight: "800", color: COLORS.text, letterSpacing: "-0.5px" }}>
                    {selectedMarket.value}
                  </span>
                  <span style={{ fontSize: "15px", fontWeight: "700", color: selectedMarket.positive ? COLORS.green : COLORS.red }}>
                    {selectedMarket.positive ? "▲" : "▼"} {selectedMarket.change}
                  </span>
                </div>
              </div>
              <button
                onClick={() => setSelectedMarket(null)}
                style={{
                  backgroundColor: COLORS.card, border: `1px solid ${COLORS.border}`,
                  color: COLORS.textMuted, borderRadius: "8px",
                  padding: "6px 12px", cursor: "pointer", fontSize: "15px",
                }}
              >✕</button>
            </div>

            {/* Color line */}
            <div style={{
              height: "3px", borderRadius: "2px", marginBottom: "16px",
              backgroundColor: selectedMarket.positive ? COLORS.green : COLORS.red,
            }} />

            {/* Detail grid */}
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr 1fr", gap: "8px", marginBottom: "12px" }}>
              {[
                { label: "Open",       value: selectedMarket.open },
                { label: "Prev Close", value: selectedMarket.prevClose },
                { label: "Day High",   value: selectedMarket.high,       color: COLORS.green },
                { label: "Day Low",    value: selectedMarket.low,        color: COLORS.red },
                { label: "52W High",   value: selectedMarket.week52High, color: COLORS.green },
                { label: "52W Low",    value: selectedMarket.week52Low,  color: COLORS.red },
                { label: "Volume",     value: selectedMarket.volume },
                { label: "Mkt Cap",    value: selectedMarket.marketCap },
              ].map((row, i) => (
                <div key={i} style={{
                  backgroundColor: COLORS.card, borderRadius: "8px",
                  padding: "10px 12px", border: `1px solid ${COLORS.border}`,
                }}>
                  <div style={{ fontSize: "10px", color: COLORS.textDim, marginBottom: "4px", fontWeight: "500" }}>
                    {row.label}
                  </div>
                  <div style={{ fontSize: "13px", color: row.color || COLORS.text, fontWeight: "700" }}>
                    {row.value || "N/A"}
                  </div>
                </div>
              ))}
            </div>

            {/* 52W range bar */}
            <div style={{
              backgroundColor: COLORS.card, borderRadius: "8px",
              padding: "12px 14px", border: `1px solid ${COLORS.border}`, marginBottom: "12px",
            }}>
              <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "6px" }}>
                <span style={{ fontSize: "10px", color: COLORS.textDim, fontWeight: "600" }}>52 WEEK RANGE</span>
                <span style={{ fontSize: "10px", color: COLORS.textMuted }}>
                  {selectedMarket.week52Low} — {selectedMarket.week52High}
                </span>
              </div>
              <div style={{ height: "5px", backgroundColor: "#1e2d45", borderRadius: "3px", overflow: "hidden" }}>
                <div style={{
                  height: "100%",
                  width: selectedMarket.rawChange >= 0 ? "68%" : "32%",
                  background: `linear-gradient(90deg, ${COLORS.red}, ${COLORS.amber}, ${COLORS.green})`,
                  borderRadius: "3px",
                }} />
              </div>
              <div style={{ display: "flex", justifyContent: "space-between", marginTop: "4px" }}>
                <span style={{ fontSize: "9px", color: COLORS.red }}>LOW</span>
                <span style={{ fontSize: "9px", color: COLORS.green }}>HIGH</span>
              </div>
            </div>

            <p style={{ margin: 0, fontSize: "10px", color: COLORS.textDim, textAlign: "center" }}>
              Data via Yahoo Finance · Refreshes every 5 min · Click outside to close
            </p>
          </div>
        </div>
      )}

      {/* ════════════════════════════════════════════
          SECTION 1: MARKET TICKER BAR
          Moneycontrol style — all items in one row
          Separated by vertical dividers
          Click any item to open detail popup
      ════════════════════════════════════════════ */}
      <div style={{ backgroundColor: "#050810", borderBottom: `1px solid ${COLORS.border}` }}>

        {/* Top strip: live status + refresh controls */}
        <div style={{
          display: "flex", justifyContent: "space-between", alignItems: "center",
          padding: "6px 20px",
          borderBottom: `1px solid ${COLORS.border}`,
          backgroundColor: "#030508",
        }}>
          <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "5px" }}>
              <div style={{
                width: "6px", height: "6px", borderRadius: "50%",
                backgroundColor: ticker.length > 0 ? COLORS.green : COLORS.amber,
                animation: "pulseDot 2s ease-in-out infinite",
              }} />
              <span style={{ fontSize: "11px", color: ticker.length > 0 ? COLORS.green : COLORS.amber, fontWeight: "600", letterSpacing: "0.5px" }}>
                {ticker.length > 0 ? "LIVE" : "LOADING"}
              </span>
            </div>
            {lastUpdated && (
              <span style={{ fontSize: "10px", color: COLORS.textDim }}>
                Updated: {lastUpdated}
              </span>
            )}
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
            <span style={{
              fontSize: "10px",
              color: countdown < 30 ? COLORS.amber : COLORS.textDim,
              fontWeight: countdown < 30 ? "600" : "400",
            }}>
              Next refresh: {Math.floor(countdown / 60)}:{String(countdown % 60).padStart(2, "0")}
            </span>
            <button
              onClick={manualRefresh}
              disabled={marketLoading}
              style={{
                display: "flex", alignItems: "center", gap: "4px",
                padding: "3px 10px",
                backgroundColor: marketLoading ? "transparent" : COLORS.accent + "22",
                border: `1px solid ${marketLoading ? COLORS.border : COLORS.accent + "66"}`,
                borderRadius: "4px",
                color: marketLoading ? COLORS.textDim : COLORS.accentHover,
                cursor: marketLoading ? "not-allowed" : "pointer",
                fontSize: "10px", fontWeight: "600",
              }}
            >
              <span style={{ animation: marketLoading ? "spin 1s linear infinite" : "none", display: "inline-block", fontSize: "11px" }}>↻</span>
              {marketLoading ? "Refreshing..." : "Refresh"}
            </button>
          </div>
        </div>

        {/* Ticker items row */}
        {ticker.length === 0 ? (
          <div style={{ display: "flex", gap: "0", padding: "10px 20px", overflowX: "auto" }}>
            {[1,2,3,4,5,6,7,8].map((i) => (
              <div key={i} style={{
                paddingRight: "20px", marginRight: "20px",
                borderRight: `1px solid ${COLORS.border}`,
              }}>
                <div style={{
                  width: "120px", height: "32px", borderRadius: "4px",
                  background: `linear-gradient(90deg, ${COLORS.card} 25%, #1e2d45 50%, ${COLORS.card} 75%)`,
                  backgroundSize: "200% 100%", animation: "shimmer 1.5s infinite",
                }} />
              </div>
            ))}
          </div>
        ) : (
          <div style={{
            display: "flex", alignItems: "center",
            overflowX: "auto", padding: "0",
            scrollbarWidth: "none",
          }}>
            {ticker.map((t, i) => (
              <div
                key={i}
                onClick={() => setSelectedMarket(t)}
                onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = "#0d1520"; }}
                onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = "transparent"; }}
                style={{
                  display: "flex", alignItems: "center", gap: "10px",
                  padding: "10px 20px",
                  borderRight: i < ticker.length - 1 ? `1px solid ${COLORS.border}` : "none",
                  cursor: "pointer", whiteSpace: "nowrap",
                  backgroundColor: "transparent",
                  transition: "background 0.15s",
                  flexShrink: 0,
                }}
              >
                <span style={{ fontSize: "12px", color: COLORS.textMuted, fontWeight: "600" }}>
                  {t.symbol}
                </span>
                <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end" }}>
                  <span style={{ fontSize: "13px", color: COLORS.text, fontWeight: "700", lineHeight: 1.2 }}>
                    {t.value}
                  </span>
                  <span style={{
                    fontSize: "11px", fontWeight: "600",
                    color: t.positive ? COLORS.green : COLORS.red,
                    display: "flex", alignItems: "center", gap: "2px",
                    lineHeight: 1.2,
                  }}>
                    {t.positive ? "▲" : "▼"} {t.change}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* ════════════════════════════════════════════
          SECTION 2: HEADER
          Logo, LIVE indicator, tagline
      ════════════════════════════════════════════ */}
      <div style={{
        padding: "32px 24px 24px", textAlign: "center",
        borderBottom: `1px solid ${COLORS.border}`,
        background: `linear-gradient(180deg, #0d1220 0%, ${COLORS.bg} 100%)`,
      }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "8px", marginBottom: "12px" }}>
          <div style={{
            width: "8px", height: "8px", borderRadius: "50%",
            backgroundColor: COLORS.green,
            animation: "pulseDot 2s ease-in-out infinite",
          }} />
          <span style={{ fontSize: "11px", color: COLORS.green, letterSpacing: "2px", fontWeight: "600" }}>LIVE</span>
        </div>
        <h1 style={{
          fontSize: "42px", fontWeight: "800", margin: "0 0 8px",
          background: "linear-gradient(135deg, #6366f1, #a855f7, #ec4899)",
          WebkitBackgroundClip: "text", WebkitTextFillColor: "transparent",
        }}>
          StockStream
        </h1>
        <p style={{ color: COLORS.textDim, fontSize: "14px", margin: 0 }}>
          AI-powered real-time financial intelligence platform
        </p>
      </div>

      {/* ════════════════════════════════════════════
          SECTION 3: SEARCH BAR + CATEGORY FILTERS
      ════════════════════════════════════════════ */}
      <div style={{ padding: "24px", borderBottom: `1px solid ${COLORS.border}`, background: COLORS.surface }}>
        <div style={{ maxWidth: "700px", margin: "0 auto" }}>
          <div style={{ display: "flex", gap: "10px", marginBottom: "16px" }}>
            <div style={{ flex: 1, position: "relative" }}>
              <span style={{
                position: "absolute", left: "16px", top: "50%",
                transform: "translateY(-50%)", color: COLORS.textDim, fontSize: "18px",
              }}>⌕</span>
              <input
                type="text"
                placeholder="Search markets, stocks, commodities..."
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                onKeyDown={(e) => { if (e.key === "Enter") doSearch(); }}
                style={{
                  width: "100%", padding: "13px 16px 13px 44px",
                  backgroundColor: COLORS.card, border: `1px solid ${COLORS.border}`,
                  borderRadius: "10px", color: COLORS.text,
                  fontSize: "14px", outline: "none", boxSizing: "border-box",
                }}
              />
            </div>
            <button
              onClick={doSearch}
              style={{
                padding: "13px 24px", backgroundColor: COLORS.accent,
                color: "white", border: "none", borderRadius: "10px",
                fontSize: "14px", cursor: "pointer", fontWeight: "600", whiteSpace: "nowrap",
              }}
            >
              Search
            </button>
          </div>
          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
            {cats.map((c) => (
              <button
                key={c}
                onClick={() => loadCategory(c)}
                style={{
                  padding: "6px 16px", borderRadius: "20px",
                  border: `1px solid ${category === c ? COLORS.accent : COLORS.border}`,
                  backgroundColor: category === c ? COLORS.accent + "22" : "transparent",
                  color: category === c ? COLORS.accentHover : COLORS.textMuted,
                  cursor: "pointer", fontSize: "12px", fontWeight: "500",
                }}
              >
                {c}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* ════════════════════════════════════════════
          SECTION 4: AI STOCK IMPACT ANALYZER
      ════════════════════════════════════════════ */}
      <div style={{
        padding: "24px",
        background: `linear-gradient(135deg, #0d0f1e, #111827)`,
        borderBottom: `1px solid ${COLORS.border}`,
      }}>
        <div style={{ maxWidth: "1100px", margin: "0 auto" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "6px" }}>
            <div style={{
              width: "28px", height: "28px", borderRadius: "8px",
              backgroundColor: COLORS.accent + "22",
              display: "flex", alignItems: "center", justifyContent: "center", fontSize: "16px",
            }}>◈</div>
            <h2 style={{ margin: 0, fontSize: "16px", fontWeight: "600", color: COLORS.text }}>
              Stock Impact Analyzer
            </h2>
            <span style={{
              fontSize: "11px", backgroundColor: COLORS.accent + "22",
              color: COLORS.accentHover, padding: "2px 10px",
              borderRadius: "20px", fontWeight: "500",
            }}>AI Powered</span>
          </div>
          <p style={{ color: COLORS.textDim, fontSize: "12px", margin: "0 0 16px 38px" }}>
            Enter any news event — get a 360° supply chain impact analysis with confidence scoring
          </p>
          <div style={{ display: "flex", gap: "10px", maxWidth: "700px" }}>
            <input
              type="text"
              placeholder="e.g. budget announced 7 new high-speed trains across India..."
              value={impactQuery}
              onChange={(e) => setImpactQuery(e.target.value)}
              onKeyDown={(e) => { if (e.key === "Enter") analyzeImpact(); }}
              style={{
                flex: 1, padding: "13px 16px",
                backgroundColor: COLORS.card, border: `1px solid ${COLORS.border}`,
                borderRadius: "10px", color: COLORS.text, fontSize: "14px", outline: "none",
              }}
            />
            <button
              onClick={analyzeImpact}
              disabled={impactLoading}
              style={{
                padding: "13px 24px",
                backgroundColor: impactLoading ? COLORS.border : "#7c3aed",
                color: "white", border: "none", borderRadius: "10px",
                fontSize: "14px", cursor: impactLoading ? "not-allowed" : "pointer",
                fontWeight: "600", whiteSpace: "nowrap",
              }}
            >
              {impactLoading ? "Analyzing..." : "Analyze Impact"}
            </button>
          </div>

          {impactLoading && <AILoader />}

          {!impactLoading && analyzed && (
            <div style={{ marginTop: "24px", animation: "fadeIn 0.5s ease" }}>
              {impacts.actualImpact.length > 0 && (
                <div style={{ marginBottom: "20px" }}>
                  <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "12px" }}>
                    <div style={{ width: "8px", height: "8px", borderRadius: "50%", backgroundColor: COLORS.green }} />
                    <span style={{ fontSize: "12px", color: COLORS.green, fontWeight: "600", letterSpacing: "0.5px" }}>
                      CONFIRMED IMPACT — HIGH CONFIDENCE
                    </span>
                    <span style={{ fontSize: "11px", color: COLORS.textDim }}>
                      ({impacts.actualImpact.length} stocks)
                    </span>
                  </div>
                  <div style={{ display: "flex", flexWrap: "wrap", gap: "12px" }}>
                    {impacts.actualImpact.map((item, i) => (
                      <StockCard key={i} item={item} type="actual" />
                    ))}
                  </div>
                </div>
              )}
              {impacts.predictedImpact.length > 0 && (
                <div>
                  <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "12px" }}>
                    <div style={{ width: "8px", height: "8px", borderRadius: "50%", backgroundColor: COLORS.amber }} />
                    <span style={{ fontSize: "12px", color: COLORS.amber, fontWeight: "600", letterSpacing: "0.5px" }}>
                      PREDICTED IMPACT — MEDIUM / LOW CONFIDENCE
                    </span>
                    <span style={{ fontSize: "11px", color: COLORS.textDim }}>
                      ({impacts.predictedImpact.length} stocks)
                    </span>
                  </div>
                  <div style={{ display: "flex", flexWrap: "wrap", gap: "12px" }}>
                    {impacts.predictedImpact.map((item, i) => (
                      <StockCard key={i} item={item} type="predicted" />
                    ))}
                  </div>
                </div>
              )}
              {impacts.actualImpact.length === 0 && impacts.predictedImpact.length === 0 && (
                <div style={{
                  padding: "16px", backgroundColor: COLORS.card,
                  borderRadius: "10px", border: `1px solid ${COLORS.border}`, textAlign: "center",
                }}>
                  <p style={{ color: COLORS.textMuted, fontSize: "13px", margin: 0 }}>
                    No significant stock impact found. Try a more specific news query.
                  </p>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* ════════════════════════════════════════════
          SECTION 5: MAIN CONTENT AREA
          Left: News Articles Grid
          Right: Stock Research Sidebar
      ════════════════════════════════════════════ */}
      <div style={{
        display: "flex", gap: "0",
        alignItems: "flex-start",
      }}>

        {/* ── LEFT: News Articles ── */}
        <div style={{ flex: 1, padding: "24px", minWidth: 0 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
            <div>
              <h2 style={{ margin: "0 0 4px", fontSize: "18px", fontWeight: "600", color: COLORS.text }}>
                {query ? `Results for "${query}"` : "Breaking News"}
              </h2>
              <p style={{ margin: 0, fontSize: "12px", color: COLORS.textDim }}>
                {news.length} articles · Auto-refreshes every 30 min
              </p>
            </div>
            {query && (
              <button
                onClick={() => { setQuery(""); loadBreaking(); }}
                style={{
                  padding: "8px 16px", backgroundColor: "transparent",
                  border: `1px solid ${COLORS.border}`, borderRadius: "8px",
                  color: COLORS.textMuted, cursor: "pointer", fontSize: "12px",
                }}
              >
                Clear search
              </button>
            )}
          </div>

          {/* Skeleton loading */}
          {loading && (
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(320px, 1fr))", gap: "16px" }}>
              {[1,2,3,4,5,6].map((i) => <SkeletonCard key={i} />)}
            </div>
          )}

          {/* News cards */}
          {!loading && news.length > 0 && (
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(320px, 1fr))", gap: "16px" }}>
              {news.map((a, idx) => {
                const cs = catStyle(a.category);
                return (
                  <div
                    key={a.id}
                    style={{
                      background: COLORS.card, borderRadius: "12px", padding: "20px",
                      border: `1px solid ${COLORS.border}`,
                      animation: `fadeIn 0.3s ease ${idx * 0.03}s both`,
                      display: "flex", flexDirection: "column", gap: "12px",
                    }}
                  >
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <span style={{
                        backgroundColor: cs.bg, color: cs.text,
                        padding: "3px 10px", borderRadius: "20px",
                        fontSize: "11px", fontWeight: "600",
                        display: "flex", alignItems: "center", gap: "5px",
                      }}>
                        <span style={{ width: "5px", height: "5px", borderRadius: "50%", backgroundColor: cs.dot, display: "inline-block" }} />
                        {a.category}
                      </span>
                      <span style={{ fontSize: "11px", color: COLORS.textDim }}>{formatDate(a.publishedAt)}</span>
                    </div>
                    <h3 style={{ margin: 0, fontSize: "14px", fontWeight: "600", color: COLORS.text, lineHeight: "1.5" }}>
                      {a.title}
                    </h3>
                    <p style={{ margin: 0, fontSize: "12px", color: COLORS.textMuted, lineHeight: "1.6", flex: 1 }}>
                      {a.description ? a.description.substring(0, 110) + "..." : "Click to read the full article."}
                    </p>
                    <div style={{
                      display: "flex", justifyContent: "space-between", alignItems: "center",
                      paddingTop: "12px", borderTop: `1px solid ${COLORS.border}`,
                    }}>
                      <span style={{ fontSize: "11px", color: COLORS.accent, fontWeight: "600" }}>
                        {a.source ? a.source.substring(0, 25) : ""}
                      </span>
                      <span
                        onClick={() => window.open(a.url, "_blank")}
                        style={{
                          backgroundColor: COLORS.accent + "22", color: COLORS.accentHover,
                          padding: "6px 14px", borderRadius: "8px", fontSize: "11px",
                          cursor: "pointer", fontWeight: "600",
                          border: `1px solid ${COLORS.accent}44`,
                        }}
                      >
                        Read →
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* Empty state */}
          {!loading && news.length === 0 && (
            <div style={{ textAlign: "center", padding: "60px 20px" }}>
              <div style={{ fontSize: "48px", marginBottom: "16px", opacity: 0.3 }}>◎</div>
              <p style={{ color: COLORS.textMuted, fontSize: "15px", margin: "0 0 20px" }}>No articles found</p>
              <button
                onClick={loadBreaking}
                style={{
                  padding: "10px 24px", backgroundColor: COLORS.accent,
                  color: "white", border: "none", borderRadius: "8px",
                  cursor: "pointer", fontSize: "14px", fontWeight: "600",
                }}
              >
                Load breaking news
              </button>
            </div>
          )}
        </div>

        {/* ── RIGHT: Stock Research Sidebar ── */}
        <div style={{
          width: "380px",
          flexShrink: 0,
          borderLeft: `1px solid ${COLORS.border}`,
          backgroundColor: COLORS.surface,
          minHeight: "100vh",
          padding: "20px",
        }}>

          {/* Search header */}
          <div style={{ marginBottom: "16px" }}>
            <div style={{ fontSize: "11px", color: COLORS.textDim, fontWeight: "600", letterSpacing: "1px", marginBottom: "10px" }}>
              STOCK DEEP DIVE
            </div>
            <div style={{ display: "flex", gap: "8px" }}>
              <input
                type="text"
                placeholder="Enter stock name e.g. TCS, Infosys..."
                value={stockQuery}
                onChange={(e) => setStockQuery(e.target.value)}
                onKeyDown={(e) => { if (e.key === "Enter") searchStock(); }}
                style={{
                  flex: 1, padding: "10px 14px",
                  backgroundColor: COLORS.card,
                  border: `1px solid ${COLORS.border}`,
                  borderRadius: "8px", color: COLORS.text,
                  fontSize: "13px", outline: "none",
                }}
              />
              <button
                onClick={searchStock}
                disabled={stockLoading}
                style={{
                  padding: "10px 14px",
                  backgroundColor: stockLoading ? COLORS.border : COLORS.accent,
                  color: "white", border: "none", borderRadius: "8px",
                  cursor: stockLoading ? "not-allowed" : "pointer",
                  fontSize: "13px", fontWeight: "600",
                }}
              >
                {stockLoading ? "..." : "Go"}
              </button>
            </div>

            {/* Quick chips */}
            <div style={{ display: "flex", gap: "6px", flexWrap: "wrap", marginTop: "8px" }}>
              {["TCS", "Reliance", "Infosys", "HDFC Bank", "ITC"].map((s) => (
                <button
                  key={s}
                  onClick={() => setStockQuery(s)}
                  style={{
                    padding: "3px 10px", fontSize: "11px",
                    backgroundColor: COLORS.card,
                    border: `1px solid ${COLORS.border}`,
                    borderRadius: "20px", color: COLORS.textMuted,
                    cursor: "pointer",
                  }}
                >
                  {s}
                </button>
              ))}
            </div>
          </div>

          {/* Loading spinner */}
          {stockLoading && (
            <div style={{ textAlign: "center", padding: "40px 0" }}>
              <div style={{ position: "relative", width: "48px", height: "48px", margin: "0 auto 12px" }}>
                <div style={{
                  width: "48px", height: "48px", borderRadius: "50%",
                  border: `2px solid ${COLORS.border}`,
                  borderTopColor: COLORS.accent,
                  animation: "spin 1s linear infinite",
                }} />
              </div>
              <p style={{ color: COLORS.accent, fontSize: "13px", margin: "0 0 4px", fontWeight: "500" }}>
                AI Researching...
              </p>
              <p style={{ color: COLORS.textDim, fontSize: "11px", margin: 0 }}>
                Analyzing fundamentals, technicals and news
              </p>
            </div>
          )}

          {/* Research results */}
          {!stockLoading && stockResearch && (
            <div style={{ animation: "fadeIn 0.4s ease" }}>

              {/* Stock header card */}
              <div style={{
                backgroundColor: COLORS.card, borderRadius: "10px",
                padding: "14px", border: `1px solid ${COLORS.border}`, marginBottom: "12px",
              }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                  <div>
                    <div style={{ fontSize: "18px", fontWeight: "800", color: COLORS.text }}>
                      {stockResearch.stockCode}
                    </div>
                    <div style={{ fontSize: "12px", color: COLORS.textMuted }}>{stockResearch.stockName}</div>
                    <div style={{ fontSize: "11px", color: COLORS.textDim, marginTop: "2px" }}>
                      {stockResearch.sector} · {stockResearch.industry}
                    </div>
                  </div>
                  <div style={{ textAlign: "right" }}>
                    <div style={{
                      width: "44px", height: "44px", borderRadius: "50%",
                      border: `3px solid ${
                        stockResearch.overallScore >= 70 ? COLORS.green :
                        stockResearch.overallScore >= 40 ? COLORS.amber : COLORS.red
                      }`,
                      display: "flex", alignItems: "center", justifyContent: "center",
                    }}>
                      <span style={{
                        fontSize: "12px", fontWeight: "800",
                        color: stockResearch.overallScore >= 70 ? COLORS.green :
                               stockResearch.overallScore >= 40 ? COLORS.amber : COLORS.red,
                      }}>
                        {stockResearch.overallScore}
                      </span>
                    </div>
                    <div style={{ fontSize: "9px", color: COLORS.textDim, marginTop: "2px" }}>SCORE</div>
                  </div>
                </div>
                <p style={{ margin: "10px 0 0", fontSize: "11px", color: COLORS.textMuted, lineHeight: "1.5" }}>
                  {stockResearch.description}
                </p>
                <div style={{ display: "flex", gap: "6px", marginTop: "8px" }}>
                  <span style={{
                    fontSize: "10px", padding: "2px 8px", borderRadius: "4px", fontWeight: "600",
                    backgroundColor: stockResearch.riskLevel === "LOW" ? "#052e16" :
                                     stockResearch.riskLevel === "HIGH" ? "#450a0a" : "#431407",
                    color: stockResearch.riskLevel === "LOW" ? COLORS.green :
                           stockResearch.riskLevel === "HIGH" ? COLORS.red : COLORS.amber,
                  }}>
                    {stockResearch.riskLevel} RISK
                  </span>
                  <span style={{
                    fontSize: "10px", padding: "2px 8px", borderRadius: "4px",
                    backgroundColor: COLORS.accent + "22", color: COLORS.accentHover, fontWeight: "600",
                  }}>
                    {stockResearch.investorType}
                  </span>
                </div>
              </div>

              {/* Analyst view */}
              {stockResearch.analystView && (
                <div style={{
                  backgroundColor: COLORS.card, borderRadius: "10px",
                  padding: "14px", border: `1px solid ${COLORS.border}`, marginBottom: "12px",
                }}>
                  <div style={{ fontSize: "10px", color: COLORS.textDim, fontWeight: "600", letterSpacing: "1px", marginBottom: "10px" }}>
                    ANALYST VIEW
                  </div>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "8px" }}>
                    <span style={{
                      fontSize: "13px", fontWeight: "700", padding: "4px 12px", borderRadius: "6px",
                      backgroundColor:
                        stockResearch.analystView.rating?.includes("BUY") ? "#052e16" :
                        stockResearch.analystView.rating?.includes("SELL") ? "#450a0a" : "#1a1a2a",
                      color:
                        stockResearch.analystView.rating?.includes("BUY") ? COLORS.green :
                        stockResearch.analystView.rating?.includes("SELL") ? COLORS.red : COLORS.amber,
                    }}>
                      {stockResearch.analystView.rating}
                    </span>
                    <div style={{ textAlign: "right" }}>
                      <div style={{ fontSize: "13px", fontWeight: "700", color: COLORS.text }}>
                        {stockResearch.analystView.targetPrice}
                      </div>
                      <div style={{ fontSize: "10px", color: COLORS.green }}>
                        {stockResearch.analystView.upside} upside
                      </div>
                    </div>
                  </div>
                  <p style={{ margin: 0, fontSize: "11px", color: COLORS.textMuted, lineHeight: "1.5" }}>
                    {stockResearch.analystView.summary}
                  </p>
                </div>
              )}

              {/* Fundamentals */}
              {stockResearch.fundamentals && (
                <div style={{
                  backgroundColor: COLORS.card, borderRadius: "10px",
                  padding: "14px", border: `1px solid ${COLORS.border}`, marginBottom: "12px",
                }}>
                  <div style={{ fontSize: "10px", color: COLORS.textDim, fontWeight: "600", letterSpacing: "1px", marginBottom: "10px" }}>
                    FUNDAMENTALS
                  </div>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "6px" }}>
                    {[
                      { label: "Market Cap",    value: stockResearch.fundamentals.marketCap },
                      { label: "P/E Ratio",     value: stockResearch.fundamentals.peRatio },
                      { label: "P/B Ratio",     value: stockResearch.fundamentals.pbRatio },
                      { label: "Debt/Equity",   value: stockResearch.fundamentals.debtToEquity },
                      { label: "ROE",           value: stockResearch.fundamentals.roe },
                      { label: "ROCE",          value: stockResearch.fundamentals.roce },
                      { label: "Div Yield",     value: stockResearch.fundamentals.dividendYield },
                      { label: "Rev Growth",    value: stockResearch.fundamentals.revenueGrowth },
                      { label: "Profit Growth", value: stockResearch.fundamentals.profitGrowth },
                      { label: "F. Rating",     value: stockResearch.fundamentals.fundamentalRating },
                    ].map((row, i) => (
                      <div key={i} style={{
                        backgroundColor: COLORS.surface, borderRadius: "6px",
                        padding: "8px 10px", border: `1px solid ${COLORS.border}`,
                      }}>
                        <div style={{ fontSize: "9px", color: COLORS.textDim, marginBottom: "2px" }}>{row.label}</div>
                        <div style={{
                          fontSize: "12px", fontWeight: "700",
                          color: row.label === "F. Rating" ?
                            (row.value === "STRONG" ? COLORS.green :
                             row.value === "WEAK" ? COLORS.red : COLORS.amber) : COLORS.text,
                        }}>
                          {row.value || "N/A"}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Technical */}
              {stockResearch.technical && (
                <div style={{
                  backgroundColor: COLORS.card, borderRadius: "10px",
                  padding: "14px", border: `1px solid ${COLORS.border}`, marginBottom: "12px",
                }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "10px" }}>
                    <div style={{ fontSize: "10px", color: COLORS.textDim, fontWeight: "600", letterSpacing: "1px" }}>
                      TECHNICAL ANALYSIS
                    </div>
                    <span style={{
                      fontSize: "11px", padding: "2px 10px", borderRadius: "4px", fontWeight: "700",
                      backgroundColor:
                        stockResearch.technical.technicalSignal === "BUY" ? "#052e16" :
                        stockResearch.technical.technicalSignal === "SELL" ? "#450a0a" : "#1a1a2a",
                      color:
                        stockResearch.technical.technicalSignal === "BUY" ? COLORS.green :
                        stockResearch.technical.technicalSignal === "SELL" ? COLORS.red : COLORS.amber,
                    }}>
                      {stockResearch.technical.technicalSignal}
                    </span>
                  </div>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "6px" }}>
                    {[
                      { label: "Trend",      value: stockResearch.technical.trend },
                      { label: "RSI",        value: stockResearch.technical.rsi },
                      { label: "MACD",       value: stockResearch.technical.macd },
                      { label: "200 DMA",    value: stockResearch.technical.movingAverage },
                      { label: "Support",    value: stockResearch.technical.support },
                      { label: "Resistance", value: stockResearch.technical.resistance },
                      { label: "Volume",     value: stockResearch.technical.volumeTrend },
                    ].map((row, i) => (
                      <div key={i} style={{
                        backgroundColor: COLORS.surface, borderRadius: "6px",
                        padding: "8px 10px", border: `1px solid ${COLORS.border}`,
                      }}>
                        <div style={{ fontSize: "9px", color: COLORS.textDim, marginBottom: "2px" }}>{row.label}</div>
                        <div style={{ fontSize: "11px", fontWeight: "700", color: COLORS.text }}>
                          {row.value || "N/A"}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* SWOT */}
              <div style={{
                backgroundColor: COLORS.card, borderRadius: "10px",
                padding: "14px", border: `1px solid ${COLORS.border}`, marginBottom: "12px",
              }}>
                <div style={{ fontSize: "10px", color: COLORS.textDim, fontWeight: "600", letterSpacing: "1px", marginBottom: "10px" }}>
                  SWOT ANALYSIS
                </div>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "6px" }}>
                  {[
                    { label: "STRENGTHS",     items: stockResearch.strengths,     color: COLORS.green, bg: "#052e16" },
                    { label: "WEAKNESSES",    items: stockResearch.weaknesses,    color: COLORS.red,   bg: "#450a0a" },
                    { label: "OPPORTUNITIES", items: stockResearch.opportunities, color: COLORS.accent, bg: COLORS.accent + "22" },
                    { label: "THREATS",       items: stockResearch.threats,       color: COLORS.amber, bg: "#431407" },
                  ].map((section, i) => (
                    <div key={i} style={{ backgroundColor: section.bg, borderRadius: "6px", padding: "8px 10px" }}>
                      <div style={{ fontSize: "9px", color: section.color, fontWeight: "700", marginBottom: "4px" }}>
                        {section.label}
                      </div>
                      {(section.items || []).map((item, j) => (
                        <div key={j} style={{ fontSize: "10px", color: COLORS.textMuted, lineHeight: "1.4", marginBottom: "2px" }}>
                          · {item}
                        </div>
                      ))}
                    </div>
                  ))}
                </div>
              </div>

              {/* Upcoming Events */}
              {stockResearch.upcomingEvents && stockResearch.upcomingEvents.length > 0 && (
                <div style={{
                  backgroundColor: COLORS.card, borderRadius: "10px",
                  padding: "14px", border: `1px solid ${COLORS.border}`, marginBottom: "12px",
                }}>
                  <div style={{ fontSize: "10px", color: COLORS.textDim, fontWeight: "600", letterSpacing: "1px", marginBottom: "10px" }}>
                    UPCOMING EVENTS
                  </div>
                  <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                    {stockResearch.upcomingEvents.map((event, i) => (
                      <div key={i} style={{
                        display: "flex", justifyContent: "space-between", alignItems: "flex-start",
                        padding: "8px 10px", borderRadius: "6px",
                        backgroundColor: COLORS.surface, border: `1px solid ${COLORS.border}`,
                      }}>
                        <div>
                          <div style={{ fontSize: "12px", fontWeight: "600", color: COLORS.text }}>
                            {event.event}
                          </div>
                          <div style={{ fontSize: "10px", color: COLORS.textDim, marginTop: "2px" }}>
                            {event.expectedDate}
                          </div>
                          <div style={{ fontSize: "10px", color: COLORS.textMuted, marginTop: "2px" }}>
                            {event.description}
                          </div>
                        </div>
                        <span style={{
                          fontSize: "9px", padding: "2px 6px", borderRadius: "4px", fontWeight: "700",
                          backgroundColor:
                            event.impact === "HIGH" ? "#450a0a" :
                            event.impact === "MEDIUM" ? "#431407" : "#1a1a2a",
                          color:
                            event.impact === "HIGH" ? COLORS.red :
                            event.impact === "MEDIUM" ? COLORS.amber : COLORS.textDim,
                          whiteSpace: "nowrap", marginLeft: "8px",
                        }}>
                          {event.impact}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Recent News */}
              {stockResearch.recentNews && stockResearch.recentNews.length > 0 && (
                <div style={{
                  backgroundColor: COLORS.card, borderRadius: "10px",
                  padding: "14px", border: `1px solid ${COLORS.border}`,
                }}>
                  <div style={{ fontSize: "10px", color: COLORS.textDim, fontWeight: "600", letterSpacing: "1px", marginBottom: "10px" }}>
                    RECENT NEWS ({stockResearch.recentNews.length})
                  </div>
                  <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                    {stockResearch.recentNews.map((item, i) => (
                      <div
                        key={i}
                        style={{
                          padding: "8px 10px", borderRadius: "6px",
                          backgroundColor: COLORS.surface,
                          border: `1px solid ${COLORS.border}`,
                          cursor: "pointer",
                        }}
                      >
                        <div style={{ fontSize: "11px", color: COLORS.text, lineHeight: "1.4", marginBottom: "4px", fontWeight: "500" }}>
                          {item.title}
                        </div>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                          <span style={{ fontSize: "10px", color: COLORS.accent }}>
                            {item.source}
                          </span>
                          <span
                            onClick={() => window.open(item.url, "_blank")}
                            style={{
                              fontSize: "10px", color: COLORS.accentHover,
                              cursor: "pointer", fontWeight: "600",
                              padding: "2px 8px", borderRadius: "4px",
                              backgroundColor: COLORS.accent + "22",
                            }}
                          >
                            Read →
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Default empty state */}
          {!stockLoading && !stockResearch && (
            <div style={{ textAlign: "center", padding: "40px 20px" }}>
              <div style={{ fontSize: "40px", marginBottom: "16px", opacity: 0.15 }}>◎</div>
              <p style={{ color: COLORS.textMuted, fontSize: "13px", margin: "0 0 8px", fontWeight: "500" }}>
                Stock Deep Dive
              </p>
              <p style={{ color: COLORS.textDim, fontSize: "11px", margin: 0, lineHeight: "1.6" }}>
                Search any Indian stock to get complete analysis — fundamentals, technicals, SWOT, upcoming events and recent news
              </p>
            </div>
          )}

        </div>
        {/* END RIGHT SIDEBAR */}

      </div>
      {/* END SECTION 5 MAIN CONTENT */}

    </div>
    // END ROOT DIV
  );
}
