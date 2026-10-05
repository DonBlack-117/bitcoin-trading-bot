# Bitcoin Trading Bot Dashboard

A real-time web dashboard for Bitcoin (BTC/MXN) trading analysis and simulation. It takes the price from Bitso and hourly candles from Binance, and applies 5 weighted technical strategies to generate buy, sell, or hold signals.

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
- **Interactive charts** — candlesticks with Bollinger Bands and RSI, computed by the backend with the same code the strategy uses
- **Global market panel** — CoinMarketCap data (top 20 cryptos, BTC/ETH dominance, Fear & Greed index)

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17 · Spring Boot 3.5 · Hibernate/JPA · Flyway |
| Database | MariaDB 11.8 (MySQL 8 also works) |
| Frontend | React 18 · Vite · ApexCharts |
| Tests | JUnit 5 · Mockito · MockMvc · Testcontainers · Playwright |
| External APIs | Bitso (BTC/MXN price) · Binance (hourly candles) · CoinMarketCap (global market) |

## Project Structure

```
bitcoin-trading-bot/
├── backend/                  # Spring Boot
│   ├── src/main/java/com/trading/bot/
│   │   ├── client/           # Bitso, Binance and CoinMarketCap clients (timeouts and retries)
│   │   ├── config/           # Bot rules, HTTP clients, scheduler, CORS
│   │   ├── controller/       # REST endpoints and error handler
│   │   ├── domain/           # Enums and money rounding
│   │   ├── indicator/        # SMA, EMA, RSI, Bollinger, ATR, OBV, VWAP
│   │   ├── service/          # Strategy, trading, portfolio and market data
│   │   ├── model/            # JPA entities
│   │   ├── repository/       # Spring Data repositories
│   │   └── dto/              # Data Transfer Objects
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── db/migration/     # Flyway migrations
│   └── src/test/             # Unit, MockMvc and Testcontainers tests
└── frontend/                 # React + Vite
    ├── src/
    │   ├── components/       # UI components
    │   ├── hooks/            # Polling and scroll helpers
    │   ├── services/         # API calls
    │   └── utils/            # Formatting and chart theme
    └── e2e/                  # Playwright tests with a mocked API
```

## Prerequisites

- Java 17+
- MariaDB 11.8 or MySQL 8+
- Node.js 18+
- Docker or Podman, only for the database tests

## Configuration

### Database

Create an empty database:

```sql
CREATE DATABASE tradingbot;
```

Flyway creates and updates the tables on startup (`backend/src/main/resources/db/migration`). Hibernate only validates them.

A database created by an older version of this project (tables made by Hibernate, no `flyway_schema_history`) is detected and marked as version 1, and then `V2` converts the amounts from `DOUBLE` to `DECIMAL` without losing rows. Take a `mysqldump` first: the way back is restoring it.

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
| `BOT_INITIAL_MXN` | `50000` | Starting capital of the simulated portfolio |
| `BOT_MIN_CONFIDENCE` | `60` | Minimum confidence (%) for the bot to open or close a trade on a signal |

> Without `CMC_API_KEY` the global market panel and the analysis assistant return 503, but everything else works normally.

## Running the App

### Backend

```bash
cd backend
./gradlew bootRun
./gradlew test   # unit, API and database tests; coverage in build/reports/jacoco
```

The server starts at `http://localhost:8080`. The first signal is ready a few seconds after startup; until then `/api/signal` answers `503 SIGNAL_NOT_READY`.

The database tests start MariaDB 11.8 with Testcontainers. With rootless Podman, enable its socket once (`systemctl --user enable --now podman.socket`); the build finds it on its own. Without Docker or Podman those tests are skipped.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The app will be available at `http://localhost:5173`.

End-to-end tests (they mock the API, so the backend is not needed):

```bash
npx playwright install chromium   # first time only
npm run test:e2e
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/ticker` | Current BTC/MXN price |
| GET | `/api/signal` | Trading signal with indicators |
| GET | `/api/history` | Signal history |
| GET | `/api/chart?limit=120` | Hourly candles with RSI and Bollinger series (30–1000) |
| GET | `/api/ohlcv?limit=120` | Hourly candles only (1–1000) |
| GET | `/api/portfolio` | Simulated portfolio state |
| GET | `/api/trades` | Last 20 trades |
| GET | `/api/trades/open` | Open trade, or 204 if there is none |
| GET | `/api/market` | Global market data |
| GET | `/api/analysis/cryptos` | Top 100 cryptocurrencies for the assistant |
| POST | `/api/analysis` | Analysis for any cryptocurrency (`{"symbol": "ETH", "amountMxn": 1000}`) |

Errors always have the same shape:

```json
{ "error": { "code": "VALIDATION_ERROR", "message": "limit no puede pasar de 1000" } }
```

| Code | Status | When |
|------|--------|------|
| `VALIDATION_ERROR` | 400 | Invalid parameter or body |
| `CRYPTO_NOT_FOUND` | 404 | The symbol is not in the CoinMarketCap top 100 |
| `UPSTREAM_ERROR` | 502 | Bitso, Binance or CoinMarketCap failed after 3 attempts |
| `CONFIG_MISSING` | 503 | `CMC_API_KEY` is not set |
| `SIGNAL_NOT_READY` | 503 | The first signal is still being calculated |
| `INTERNAL_ERROR` | 500 | Unexpected error (details only in the server log) |

## Known limitation

Binance candles are in USD and are converted to pesos with today's implied rate (Bitso price ÷ last Binance close). The shape of the chart and every indicator are unaffected, but the peso level of older candles is approximate.

## Disclaimer

This project is for educational purposes only. It does not constitute financial advice. The generated signals are simulations based on technical indicators and do not guarantee real-world results.
