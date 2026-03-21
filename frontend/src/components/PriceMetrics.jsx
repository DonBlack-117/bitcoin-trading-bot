import './PriceMetrics.css'

function PriceMetrics({ ticker }) {
  if (!ticker) return null

  const { last, ask, bid, change24h } = ticker

  const btcPer100 = last > 0 ? (100 / last) : 0
  const changeColor = change24h >= 0 ? '#22c55e' : '#ef4444'
  const changeSign = change24h >= 0 ? '+' : ''

  return (
    <div className="price-metrics card">
      <div className="metrics-header">
        <span className="metrics-title">Métricas de Precio</span>
        <span className="change-badge" style={{ color: changeColor }}>
          {changeSign}{change24h?.toFixed(2)}% 24h
        </span>
      </div>

      <div className="metrics-grid">
        <div className="metric-item">
          <div className="metric-label">Precio Actual</div>
          <div className="metric-value">
            {formatMxn(last)}
          </div>
          <div className="metric-sub">MXN por BTC</div>
        </div>

        <div className="metric-item">
          <div className="metric-label">Con $100 MXN compras</div>
          <div className="metric-value metric-small">
            {btcPer100.toFixed(8)} BTC
          </div>
          <div className="metric-sub">equivalente aproximado</div>
        </div>

        <div className="metric-item metric-buy">
          <div className="metric-label">Precio de Compra (Ask)</div>
          <div className="metric-value" style={{ color: '#ef4444' }}>
            {formatMxn(ask)}
          </div>
          <div className="metric-sub">precio al que puedes comprar</div>
        </div>

        <div className="metric-item metric-sell">
          <div className="metric-label">Precio de Venta (Bid)</div>
          <div className="metric-value" style={{ color: '#22c55e' }}>
            {formatMxn(bid)}
          </div>
          <div className="metric-sub">precio al que puedes vender</div>
        </div>
      </div>
    </div>
  )
}

function formatMxn(value) {
  if (!value) return '$0'
  return new Intl.NumberFormat('es-MX', {
    style: 'currency',
    currency: 'MXN',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
}

export default PriceMetrics
