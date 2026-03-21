# Bitcoin Trading Bot Dashboard

Dashboard web para análisis y simulación de trading de Bitcoin (BTC/MXN) en tiempo real. Obtiene datos de mercado de Bitso y aplica 5 estrategias técnicas ponderadas para generar señales de compra, venta o mantener.

## Características

- **Señales en tiempo real** — precio actual, RSI, MACD, Bollinger Bands, EMA, VWAP, soporte y resistencia
- **5 estrategias técnicas ponderadas** con sistema de votación (-3 a +3):
  - EMA Crossover (tendencia SMA 50/200)
  - RSI + Bollinger Bands (sobrecomprado/sobrevendido)
  - MACD (momentum)
  - Volumen + VWAP (confirmación de volumen)
  - Soporte / Resistencia (niveles clave)
- **Portafolio simulado** — 50,000 MXN de capital inicial, 10% por operación, Stop Loss y Take Profit automáticos basados en ATR
- **Historial de operaciones** — últimas 20 trades con P&L, razón de cierre y tasa de éxito
- **Asistente de análisis** — analiza cualquier criptomoneda con escenarios proyectados (optimista, esperado, riesgo)
- **Gráficas interactivas** — velas OHLCV y RSI con ApexCharts
- **Mercado global** — datos de CoinMarketCap (top 20 criptos, dominancia BTC/ETH, Fear & Greed)

## Stack

| Capa | Tecnología |
|------|-----------|
| Backend | Java 17 · Spring Boot 3.3 · Hibernate/JPA |
| Base de datos | MySQL 8 |
| Frontend | React 18 · Vite · ApexCharts |
| APIs externas | Bitso (precio BTC/MXN) · CoinMarketCap (mercado global) |

## Estructura del proyecto

```
bitcoin-trading-bot/
├── backend/                  # Spring Boot
│   ├── src/main/java/com/trading/bot/
│   │   ├── controller/       # REST endpoints
│   │   ├── service/          # Lógica de negocio y estrategias
│   │   ├── model/            # Entidades JPA
│   │   ├── repository/       # Spring Data repositories
│   │   └── dto/              # Data Transfer Objects
│   └── src/main/resources/
│       └── application.properties
└── frontend/                 # React + Vite
    └── src/
        ├── components/       # Componentes UI
        ├── services/         # Llamadas a la API
        └── utils/            # Indicadores técnicos
```

## Requisitos previos

- Java 17+
- MySQL 8+
- Node.js 18+

## Configuración

### Base de datos

Crea la base de datos en MySQL:

```sql
CREATE DATABASE tradingbot;
```

### Variables de entorno (opcionales)

| Variable | Default | Descripción |
|----------|---------|-------------|
| `DATABASE_URL` | `jdbc:mysql://localhost:3306/tradingbot` | URL de conexión MySQL |
| `DB_USER` | `root` | Usuario MySQL |
| `DB_PASSWORD` | `root` | Contraseña MySQL |
| `CMC_API_KEY` | _(vacío)_ | API Key de CoinMarketCap (opcional) |

> Sin `CMC_API_KEY` el panel de mercado global no mostrará datos, pero el resto funciona con normalidad.

## Ejecución

### Backend

```bash
cd backend
./gradlew bootRun
```

El servidor inicia en `http://localhost:8080`. Hibernate crea las tablas automáticamente al primer arranque.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

La app estará disponible en `http://localhost:5173`.

## Endpoints principales

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/ticker` | Precio actual BTC/MXN |
| GET | `/api/signal` | Señal de trading con indicadores |
| GET | `/api/history` | Historial de señales |
| GET | `/api/ohlcv` | Datos de velas OHLCV |
| GET | `/api/portfolio` | Estado del portafolio simulado |
| GET | `/api/trades` | Últimas 20 operaciones |
| GET | `/api/market` | Datos de mercado global |
| POST | `/api/analysis` | Análisis de cualquier criptomoneda |

## Aviso

Este proyecto es únicamente educativo. No constituye asesoría financiera. Las señales generadas son simulaciones basadas en indicadores técnicos y no garantizan resultados reales.
