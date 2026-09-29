const mxnFormatters = {}

function mxnFormatter(digits) {
  if (!mxnFormatters[digits]) {
    mxnFormatters[digits] = new Intl.NumberFormat('es-MX', {
      style: 'currency',
      currency: 'MXN',
      minimumFractionDigits: digits,
      maximumFractionDigits: digits,
    })
  }
  return mxnFormatters[digits]
}

const isNum = (v) => typeof v === 'number' && !Number.isNaN(v)

export function fmtMxn(value, digits = 2) {
  if (!isNum(value)) return '—'
  return mxnFormatter(digits).format(value)
}

export function fmtNum(value, digits = 2) {
  if (!isNum(value)) return '—'
  return value.toLocaleString('es-MX', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

export function fmtPct(value, digits = 2) {
  if (!isNum(value)) return '—'
  return `${value >= 0 ? '+' : ''}${value.toFixed(digits)}%`
}

export function fmtBtc(value, digits = 8) {
  if (!isNum(value)) return '—'
  return value.toFixed(digits)
}

export function fmtUsd(value) {
  if (!isNum(value)) return '—'
  if (value >= 1) {
    return `$${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
  }
  return `$${value.toFixed(6)}`
}

export function fmtTime(date) {
  if (!date) return '—'
  return date.toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

export function fmtDateTime(value) {
  if (!value) return '—'
  return new Date(value).toLocaleString('es-MX', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/** 'up' | 'down' | 'flat' según el signo del valor. */
export function trendOf(value) {
  if (!isNum(value) || value === 0) return 'flat'
  return value > 0 ? 'up' : 'down'
}

/** Tono visual de una señal del backend: 'buy' | 'sell' | 'hold'. */
export function toneOfSignal(signal, cssClass) {
  if (cssClass === 'signal-buy') return 'buy'
  if (cssClass === 'signal-sell') return 'sell'
  if (cssClass === 'signal-hold') return 'hold'
  if (signal === 'COMPRAR') return 'buy'
  if (signal === 'VENDER') return 'sell'
  return 'hold'
}
