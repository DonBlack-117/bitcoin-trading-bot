const BASE = '/api'

/** Error del backend con su código estable (VALIDATION_ERROR, UPSTREAM_ERROR…). */
export class ApiError extends Error {
  constructor(message, code, status) {
    super(message)
    this.code = code
    this.status = status
  }
}

const TIMEOUT_MS = 15000

async function request(path, options = {}) {
  // Una petición colgada no deja el polling esperando indefinidamente. Si el llamador pasa su propio
  // signal (para cancelar), se combinan con AbortSignal.any.
  const timeout = AbortSignal.timeout(TIMEOUT_MS)
  const signal = options.signal ? AbortSignal.any([options.signal, timeout]) : timeout

  let response
  try {
    response = await fetch(`${BASE}${path}`, { ...options, signal })
  } catch (err) {
    if (options.signal?.aborted) {
      throw new ApiError('Petición cancelada', 'ABORTED', 0)
    }
    if (err?.name === 'TimeoutError') {
      throw new ApiError('El servidor tardó demasiado en responder', 'TIMEOUT', 0)
    }
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
export const fetchCryptoList = (signal) => request('/analysis/cryptos', { signal })

/** Última señal, o null mientras el backend calcula la primera (SIGNAL_NOT_READY). */
export async function fetchSignal() {
  try {
    return await request('/signal')
  } catch (err) {
    if (err.code === 'SIGNAL_NOT_READY') return null
    throw err
  }
}

export function fetchAnalysis(symbol, amountMxn, signal) {
  return request('/analysis', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ symbol, amountMxn }),
    signal,
  })
}
