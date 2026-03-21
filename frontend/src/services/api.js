const BASE = '/api'

async function handleResponse(response) {
  if (!response.ok) {
    const text = await response.text().catch(() => '')
    throw new Error(text || `Error del servidor (${response.status})`)
  }
  return response.json()
}

export async function fetchTicker() {
  const response = await fetch(`${BASE}/ticker`)
  return handleResponse(response)
}

export async function fetchOhlcv(limit = 120) {
  const response = await fetch(`${BASE}/ohlcv?limit=${limit}`)
  return handleResponse(response)
}

export async function fetchSignal() {
  const response = await fetch(`${BASE}/signal`)
  return handleResponse(response)
}

export async function fetchMarket() {
  const response = await fetch(`${BASE}/market`)
  return handleResponse(response)
}

export async function fetchHistory() {
  const response = await fetch(`${BASE}/history`)
  return handleResponse(response)
}

export async function fetchTrades() {
  const response = await fetch(`${BASE}/trades`)
  return handleResponse(response)
}

export async function fetchOpenTrade() {
  const response = await fetch(`${BASE}/trades/open`)
  if (response.status === 204) return null
  return handleResponse(response)
}

export async function fetchPortfolio() {
  const response = await fetch(`${BASE}/portfolio`)
  return handleResponse(response)
}

export async function fetchCryptoList() {
  const response = await fetch(`${BASE}/analysis/cryptos`)
  return handleResponse(response)
}

export async function fetchAnalysis(symbol, amountMxn) {
  const response = await fetch(`${BASE}/analysis`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ symbol, amountMxn }),
  })
  return handleResponse(response)
}
