# Bitcoin Trading Bot Dashboard

A real-time web dashboard for Bitcoin (BTC/MXN) trading analysis and simulation. Fetches live market data from Bitso and applies 5 weighted technical strategies to generate buy, sell, or hold signals.

## Features

- **Real-time signals** — current price, RSI, MACD, Bollinger Bands, EMA, VWAP, support and resistance levels
- **5 weighted technical strategies** with a voting system (-3 to +3):
  - EMA Crossover (SMA 50/200 trend)
  - RSI + Bollinger Bands (overbought/oversold)
  - MACD (momentum)
  - Volume + VWAP (volume confirmation)
  - Support / Resistance (key price levels)
- **Simulated portfolio** — 50,000 MXN initial capital, 10% per trade, automatic Stop Loss and Take Profit based on ATR
- **Trade history** — last 20 trades with P&L, close reason, and win rate
- **Analysis assistant** — analyzes any cryptocurrency with projected scenarios (optimistic, expected, risk)
- **Interactive charts** — OHLCV candlesticks and RSI powered by ApexCharts
- **Global market panel** — CoinMarketCap data (top 20 cryptos, BTC/ETH dominance, Fear & Greed index)

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17 · Spring Boot 3.3 · Hibernate/JPA |
| Database | MySQL 8 |
| Frontend | React 18 · Vite · ApexCharts |
| External APIs | Bitso (BTC/MXN price) · CoinMarketCap (global market) |

## Project Structure

```
bitcoin-trading-bot/
├── backend/                  # Spring Boot
│   ├── src/main/java/com/trading/bot/
│   │   ├── controller/       # REST endpoints
│   │   ├── service/          # Business logic and strategies
│   │   ├── model/            # JPA entities
│   │   ├── repository/       # Spring Data repositories
│   │   └── dto/              # Data Transfer Objects
│   └── src/main/resources/
│       └── application.properties
└── frontend/                 # React + Vite
    └── src/
        ├── components/       # UI components
        ├── services/         # API calls
        └── utils/            # Technical indicators
```

## Prerequisites

- Java 17+
- MySQL 8+
- Node.js 18+

## Configuration

### Database

Create the database in MySQL:

```sql
CREATE DATABASE tradingbot;
```

### Local credentials

Create `backend/src/main/resources/application-local.properties` (it is in `.gitignore` and is loaded automatically):

```properties
spring.datasource.password=${DB_PASSWORD:your-db-password}
cmc.api.key=${CMC_API_KEY:your-coinmarketcap-key}
```

### Environment Variables

Environment variables override the local file.

| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `jdbc:mysql://localhost:3306/tradingbot` | MySQL/MariaDB connection URL |
| `DB_USER` | `root` | Database username |
| `DB_PASSWORD` | _(empty)_ | Database password |
| `CMC_API_KEY` | _(empty)_ | CoinMarketCap API key (optional) |
| `BINANCE_API_URL` | `https://data-api.binance.vision` | Binance market data host (the public mirror avoids the 451 region block) |
| `BOT_SIGNAL_INTERVAL_MS` | `30000` | How often the bot recalculates the signal and processes trades |

> Without `CMC_API_KEY` the global market panel and the analysis assistant return 503, but everything else works normally.

## Running the App

### Backend

```bash
cd backend
./gradlew bootRun
./gradlew test   # unit tests for the strategies, Bitso helpers and signal history
```

The server starts at `http://localhost:8080`. Hibernate automatically creates the tables on first startup.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The app will be available at `http://localhost:5173`.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/ticker` | Current BTC/MXN price |
| GET | `/api/signal` | Trading signal with indicators |
| GET | `/api/history` | Signal history |
| GET | `/api/ohlcv` | OHLCV candlestick data |
| GET | `/api/portfolio` | Simulated portfolio state |
| GET | `/api/trades` | Last 20 trades |
| GET | `/api/market` | Global market data |
| POST | `/api/analysis` | Analysis for any cryptocurrency |

## Disclaimer

This project is for educational purposes only. It does not constitute financial advice. The generated signals are simulations based on technical indicators and do not guarantee real-world results.
