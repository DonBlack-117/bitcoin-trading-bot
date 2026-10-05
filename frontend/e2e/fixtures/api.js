/** Respuestas de ejemplo con la forma real de la API del backend. */

const HOUR = 3600
const START = 1_790_000_000

function candles(count) {
  const list = []
  let price = 1_500_000
  for (let i = 0; i < count; i++) {
    const open = price
    price = price * (1 + Math.sin(i / 6) * 0.004)
    list.push({
      timestamp: START + i * HOUR,
      open,
      high: Math.max(open, price) * 1.002,
      low: Math.min(open, price) * 0.998,
      close: price,
      volume: 400 + i,
    })
  }
  return list
}

const chartCandles = candles(120)

export const data = {
  ticker: { last: 1_570_590, ask: 1_570_480, bid: 1_569_950, volume: 2.55, change24h: 2.06 },
  signal: {
    signal: 'COMPRAR',
    confidence: 74,
    actionable: true,
    description: 'Señal de compra con 74% confianza. RSI: 32.0 | Puntaje alcista: 9 vs bajista: 1',
    votes: { emaCruce: 2, rsiBollinger: 2, macd: 3, volumenVwap: 2, soporteResistencia: -1 },
    indicators: {
      rsi: 32, macdLine: 10, macdSignal: 5, macdHistogram: 5, bollingerUpper: 1_600_000,
      bollingerMiddle: 1_560_000, bollingerLower: 1_520_000, atr: 9_000, vwap: 1_565_000,
      support: 1_530_000, resistance: 1_610_000, sma20: 1_560_000, sma50: 1_550_000,
      ema12: 1_565_000, ema26: 1_560_000,
    },
    rsi: 32,
    price: 1_570_590,
    scoreBuy: 9,
    scoreSell: 1,
    calculatedAt: '2026-10-04T23:55:56Z',
  },
  history: [
    { id: 2, timestamp: '2026-10-04T17:55:56', signal: 'COMPRAR', price: 1_570_590, confidence: 74, scoreBuy: 9, scoreSell: 1 },
    { id: 1, timestamp: '2026-05-13T02:51:05', signal: 'VENDER', price: 1_398_490, confidence: 62, scoreBuy: 1, scoreSell: 7 },
  ],
  trades: [],
  portfolio: {
    initialCapital: 50000, mxnBalance: 45000, btcBalance: 0.00333333, btcPrice: 1_570_590,
    totalValueMxn: 50235.29, unrealizedPnl: 235.29, totalReturn: 235.29, totalReturnPct: 0.47,
    totalTrades: 0, winningTrades: 0, winRate: 0,
  },
  chart: {
    candles: chartCandles,
    rsi: chartCandles.slice(14).map((c, i) => ({ timestamp: c.timestamp, value: 40 + (i % 30) })),
    bollinger: chartCandles.slice(19).map((c) => ({
      timestamp: c.timestamp, upper: c.close * 1.01, middle: c.close, lower: c.close * 0.99,
    })),
  },
  market: {
    totalMarketCap: 2.94e12, totalVolume24h: 4.6e10, btcDominance: 59.1, ethDominance: 11.3,
    activeCryptos: 8131, activeExchanges: 978, marketCapChange24h: 1.65,
    topCryptos: [
      { rank: 1, symbol: 'BTC', name: 'Bitcoin', price: 86431, marketCap: 1.7e12, volume24h: 1.5e10, pct1h: 0.02, pct24h: 2.0, pct7d: 3.1 },
      { rank: 2, symbol: 'ETH', name: 'Ethereum', price: 2700, marketCap: 3.2e11, volume24h: 1.1e10, pct1h: -0.1, pct24h: 1.4, pct7d: 1.3 },
    ],
    fearGreedScore: 54,
    fearGreedLabel: 'Neutral',
  },
  cryptos: [
    { rank: 1, symbol: 'BTC', name: 'Bitcoin', price: 1_570_000, marketCap: 3e13, volume24h: 3e11, pct1h: 0.1, pct24h: 2, pct7d: 3 },
    { rank: 2, symbol: 'ETH', name: 'Ethereum', price: 49_500, marketCap: 6e12, volume24h: 1e11, pct1h: -0.2, pct24h: 1.4, pct7d: 1.3 },
  ],
  analysis: {
    symbol: 'ETH', name: 'Ethereum', rank: 2, priceMxn: 49_500, pct1h: 2, pct24h: 7, pct7d: 15,
    volume24h: 1e11, marketCap: 6e12, signal: 'COMPRAR', strong: true, confidence: 92,
    reasons: ['En las últimas 24h Ethereum acumuló un avance de 7.00%'],
    amountMxn: 1000, unitsToBuy: 0.0202, optimisticMxn: 1140, expectedMxn: 1035, riskMxn: 944,
    optimisticPct: 14, expectedPct: 3.5, riskPct: -5.6,
  },
}

export function apiError(code, message) {
  return { error: { code, message } }
}

/**
 * Responde todas las rutas /api/** con los datos de ejemplo.
 * `overrides` cambia una ruta: { '/signal': { status: 503, body: {...} } } o una función (route) => {}.
 * Devuelve un registro de las rutas pedidas para revisar qué consultó la página.
 */
export async function mockApi(page, overrides = {}) {
  const calls = []
  const routes = {
    '/ticker': data.ticker,
    '/signal': data.signal,
    '/history': data.history,
    '/trades': data.trades,
    '/portfolio': data.portfolio,
    '/chart': data.chart,
    '/market': data.market,
    '/analysis/cryptos': data.cryptos,
    '/analysis': data.analysis,
  }

  await page.route('**/api/**', async (route) => {
    const path = new URL(route.request().url()).pathname.replace(/^\/api/, '')
    calls.push(path)
    const override = overrides[path]
    if (typeof override === 'function') return override(route)
    if (override) {
      return route.fulfill({ status: override.status ?? 200, json: override.body })
    }
    if (!(path in routes)) {
      return route.fulfill({ status: 404, json: apiError('NOT_FOUND', `No existe ${path}`) })
    }
    return route.fulfill({ json: routes[path] })
  })

  return calls
}
