const BASE = '/api'

/** Error del backend con su código estable (VALIDATION_ERROR, UPSTREAM_ERROR…). */
export class ApiError extends Error {
  constructor(message, code, status) {
    super(message)
    this.code = code
    this.status = status
  }
}

async function request(path, options) {
  let response
  try {
    response = await fetch(`${BASE}${path}`, options)
  } catch {
    throw new ApiError('No hay conexión con el servidor', 'NETWORK_ERROR', 0)
  }
  if (response.status === 204) return null
  if (!response.ok) {
    // El backend responde {"error": {"code", "message"}}; el proxy de Vite puede mandar texto
    const body = await response.json().catch(() => null)
    const error = body?.error
    throw new ApiError(
      error?.message || `Error del servidor (${response.status})`,
      error?.code || 'HTTP_ERROR',
      response.status,
    )
  }
  return response.json()
}

export const fetchTicker = () => request('/ticker')
export const fetchChart = (limit = 120) => request(`/chart?limit=${limit}`)
export const fetchMarket = () => request('/market')
export const fetchHistory = () => request('/history')
export const fetchTrades = () => request('/trades')
export const fetchOpenTrade = () => request('/trades/open')
export const fetchPortfolio = () => request('/portfolio')
export const fetchCryptoList = () => request('/analysis/cryptos')

/** Última señal, o null mientras el backend calcula la primera (SIGNAL_NOT_READY). */
export async function fetchSignal() {
  try {
    return await request('/signal')
  } catch (err) {
    if (err.code === 'SIGNAL_NOT_READY') return null
    throw err
  }
}

export function fetchAnalysis(symbol, amountMxn) {
  return request('/analysis', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ symbol, amountMxn }),
  })
}
