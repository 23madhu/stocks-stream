import { useState, useEffect } from "react";
import axios from "axios";

const API = "http://localhost:8080/api/news";

function App() {
  const [news, setNews] = useState([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [category, setCategory] = useState("ALL");

  useEffect(() => {
    // eslint-disable-next-line react-hooks/immutability
    loadBreaking();
  }, []);

  async function loadBreaking() {
    setLoading(true);
    try {
      const res = await axios.get(API + "/breaking");
      setNews(res.data);
    } catch (e) {
      console.log(e);
    }
    setLoading(false);
  }

  async function doSearch() {
    if (!query.trim()) return;
    setLoading(true);
    try {
      const res = await axios.get(API + "/search?q=" + query);
      setNews(res.data);
    } catch (e) {
      console.log(e);
    }
    setLoading(false);
  }

  async function loadCategory(cat) {
    setCategory(cat);
    setLoading(true);
    try {
      if (cat === "ALL") {
        const res = await axios.get(API + "/breaking");
        setNews(res.data);
      } else {
        const res = await axios.get(API + "/category/" + cat);
        setNews(res.data);
      }
    } catch (e) {
      console.log(e);
    }
    setLoading(false);
  }

  function formatDate(d) {
    if (!d) return "";
    return new Date(d).toLocaleDateString("en-IN", {
      day: "numeric",
      month: "short",
      year: "numeric",
    });
  }

  const headerStyle = {
    backgroundColor: "#1a1a2e",
    padding: "20px",
    textAlign: "center",
  };

  const titleStyle = {
    color: "#e94560",
    fontSize: "2.5rem",
    margin: 0,
  };

  const subStyle = {
    color: "#a8a8b3",
    margin: "5px 0 0 0",
  };

  const searchBarStyle = {
    backgroundColor: "#16213e",
    padding: "20px",
    textAlign: "center",
  };

  const inputRowStyle = {
    display: "flex",
    justifyContent: "center",
    gap: "10px",
    maxWidth: "700px",
    margin: "0 auto",
  };

  const inputStyle = {
    flex: 1,
    padding: "12px 20px",
    borderRadius: "25px",
    border: "none",
    fontSize: "1rem",
    outline: "none",
  };

  const btnRedStyle = {
    padding: "12px 25px",
    backgroundColor: "#e94560",
    color: "white",
    border: "none",
    borderRadius: "25px",
    fontSize: "1rem",
    cursor: "pointer",
    fontWeight: "bold",
  };

  const catRowStyle = {
    marginTop: "15px",
    display: "flex",
    justifyContent: "center",
    gap: "10px",
  };

  const gridStyle = {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fill, minmax(350px, 1fr))",
    gap: "20px",
  };

  const cardStyle = {
    backgroundColor: "white",
    borderRadius: "12px",
    padding: "20px",
    boxShadow: "0 2px 10px rgba(0,0,0,0.08)",
    borderLeft: "4px solid #e94560",
  };

  const cats = ["ALL", "STOCKS", "GLOBAL", "COMMODITIES", "CRYPTO", "TECH", "GENERAL"];

  return (
    <div style={{ fontFamily: "Arial, sans-serif", minHeight: "100vh", backgroundColor: "#f0f2f5" }}>

      <div style={headerStyle}>
        <h1 style={titleStyle}>StockStream</h1>
        <p style={subStyle}>Real-time Stock and Financial News</p>
      </div>

      <div style={searchBarStyle}>
        <div style={inputRowStyle}>
          <input
            style={inputStyle}
            type="text"
            placeholder="Search: gold news, ITC stock, silver price..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => { if (e.key === "Enter") doSearch(); }}
          />
          <button style={btnRedStyle} onClick={doSearch}>
            Search
          </button>
        </div>

        <div style={catRowStyle}>
          {cats.map((c) => (
            <button
              key={c}
              onClick={() => loadCategory(c)}
              style={{
                padding: "8px 20px",
                borderRadius: "20px",
                border: "2px solid",
                borderColor: category === c ? "#e94560" : "#a8a8b3",
                backgroundColor: category === c ? "#e94560" : "transparent",
                color: category === c ? "white" : "#a8a8b3",
                cursor: "pointer",
                fontWeight: "bold",
                fontSize: "0.9rem",
              }}
            >
              {c}
            </button>
          ))}
        </div>
      </div>

      <div style={{ padding: "30px", maxWidth: "1200px", margin: "0 auto" }}>
        <h2 style={{ color: "#1a1a2e", marginBottom: "20px" }}>
          Breaking News
          <span style={{ fontSize: "1rem", color: "#666", marginLeft: "10px" }}>
            ({news.length} articles)
          </span>
        </h2>

        {loading && (
          <div style={{ textAlign: "center", padding: "50px", color: "#666" }}>
            Loading news...
          </div>
        )}

        {!loading && (
          <div style={gridStyle}>
            {news.map((a) => (
              <div key={a.id} style={cardStyle}>

                <span style={{
                  backgroundColor: a.category === "GOLD" ? "#ffd700" : a.category === "GLOBAL" ? "#4CAF50" : "#2196F3",
                  color: a.category === "GOLD" ? "#333" : "white",
                  padding: "3px 10px",
                  borderRadius: "12px",
                  fontSize: "0.75rem",
                  fontWeight: "bold",
                }}>
                  {a.category}
                </span>

                <h3 style={{
                  fontSize: "1rem",
                  fontWeight: "bold",
                  color: "#1a1a2e",
                  margin: "10px 0 8px 0",
                  lineHeight: "1.4",
                }}>
                  {a.title}
                </h3>

                <p style={{
                  fontSize: "0.875rem",
                  color: "#666",
                  lineHeight: "1.5",
                  margin: "0 0 15px 0",
                }}>
                  {a.description ? a.description.substring(0, 120) + "..." : "Click to read more..."}
                </p>

                <div style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  borderTop: "1px solid #f0f0f0",
                  paddingTop: "12px",
                }}>
                  <span style={{ fontSize: "0.8rem", color: "#e94560", fontWeight: "bold" }}>
                    {a.source}
                  </span>
                  <div style={{ display: "flex", gap: "10px", alignItems: "center" }}>
                    <span style={{ fontSize: "0.75rem", color: "#999" }}>
                      {formatDate(a.publishedAt)}
                    </span>
                    
                      <a href={a.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      style={{
                        backgroundColor: "#e94560",
                        color: "white",
                        padding: "5px 12px",
                        borderRadius: "15px",
                        fontSize: "0.75rem",
                        textDecoration: "none",
                        fontWeight: "bold",
                      }}
                    >
                      Read
                    </a>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {!loading && news.length === 0 && (
          <div style={{ textAlign: "center", padding: "50px", color: "#666" }}>
            <p>No news found.</p>
            <button style={btnRedStyle} onClick={loadBreaking}>
              Show Breaking News
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default App;