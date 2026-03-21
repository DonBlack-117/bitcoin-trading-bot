/**
 * Simple Moving Average
 */
export function sma(prices, period) {
  const result = []
  for (let i = period - 1; i < prices.length; i++) {
    const slice = prices.slice(i - period + 1, i + 1)
    const avg = slice.reduce((a, b) => a + b, 0) / period
    result.push(avg)
  }
  return result
}

/**
 * Exponential Moving Average
 */
export function ema(prices, period) {
  if (prices.length < period) return []
  const result = []
  const multiplier = 2 / (period + 1)

  let prevEma = prices.slice(0, period).reduce((a, b) => a + b, 0) / period
  result.push(prevEma)

  for (let i = period; i < prices.length; i++) {
    const currentEma = (prices[i] - prevEma) * multiplier + prevEma
    result.push(currentEma)
    prevEma = currentEma
  }

  return result
}

/**
 * RSI (Wilder's method), period=14
 */
export function rsi(prices, period = 14) {
  const result = []
  if (prices.length <= period) return result

  let avgGain = 0
  let avgLoss = 0

  for (let i = 1; i <= period; i++) {
    const change = prices[i] - prices[i - 1]
    if (change > 0) avgGain += change
    else avgLoss += Math.abs(change)
  }

  avgGain /= period
  avgLoss /= period

  if (avgLoss === 0) {
    result.push(100)
  } else {
    const rs = avgGain / avgLoss
    result.push(100 - 100 / (1 + rs))
  }

  for (let i = period + 1; i < prices.length; i++) {
    const change = prices[i] - prices[i - 1]
    const gain = Math.max(change, 0)
    const loss = Math.abs(Math.min(change, 0))

    avgGain = (avgGain * (period - 1) + gain) / period
    avgLoss = (avgLoss * (period - 1) + loss) / period

    if (avgLoss === 0) {
      result.push(100)
    } else {
      const rs = avgGain / avgLoss
      result.push(100 - 100 / (1 + rs))
    }
  }

  return result
}

/**
 * Bollinger Bands
 * Returns { upper, middle, lower } for the last candle
 */
export function bollingerBands(prices, period = 20) {
  if (prices.length < period) return { upper: 0, middle: 0, lower: 0 }

  const window = prices.slice(prices.length - period)
  const mean = window.reduce((a, b) => a + b, 0) / period
  const variance = window.reduce((acc, p) => acc + Math.pow(p - mean, 2), 0) / period
  const std = Math.sqrt(variance)

  return {
    upper: mean + 2 * std,
    middle: mean,
    lower: mean - 2 * std,
  }
}

/**
 * Compute RSI series aligned with candle timestamps
 * Returns array of { x: timestamp, y: rsiValue }
 */
export function computeRsiSeries(candles, period = 14) {
  const closes = candles.map(c => c.close)
  const rsiValues = rsi(closes, period)

  // RSI values start at index `period`
  const startIndex = period
  return rsiValues.map((val, i) => ({
    x: candles[startIndex + i].timestamp * 1000,
    y: parseFloat(val.toFixed(2)),
  }))
}

/**
 * Compute Bollinger Band series for chart annotations
 */
export function computeBollingerSeries(candles, period = 20) {
  const closes = candles.map(c => c.close)
  const upper = []
  const middle = []
  const lower = []

  for (let i = period - 1; i < closes.length; i++) {
    const window = closes.slice(i - period + 1, i + 1)
    const mean = window.reduce((a, b) => a + b, 0) / period
    const variance = window.reduce((acc, p) => acc + Math.pow(p - mean, 2), 0) / period
    const std = Math.sqrt(variance)

    const ts = candles[i].timestamp * 1000
    upper.push({ x: ts, y: parseFloat((mean + 2 * std).toFixed(2)) })
    middle.push({ x: ts, y: parseFloat(mean.toFixed(2)) })
    lower.push({ x: ts, y: parseFloat((mean - 2 * std).toFixed(2)) })
  }

  return { upper, middle, lower }
}

/**
 * Format large numbers for display
 */
export function formatLargeNumber(num) {
  if (num >= 1e12) return `$${(num / 1e12).toFixed(2)}T`
  if (num >= 1e9) return `$${(num / 1e9).toFixed(2)}B`
  if (num >= 1e6) return `$${(num / 1e6).toFixed(2)}M`
  return `$${num.toFixed(2)}`
}

/**
 * Format percentage
 */
export function formatPct(val) {
  if (val === null || val === undefined) return 'N/A'
  const fixed = parseFloat(val).toFixed(2)
  return val >= 0 ? `+${fixed}%` : `${fixed}%`
}
