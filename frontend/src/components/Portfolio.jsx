import './Portfolio.css'

function fmt(n, decimals = 2) {
  if (n == null) return '—'
  return n.toLocaleString('es-MX', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })
}

function fmtMxn(n) {
  if (n == null) return '—'
  return '$' + fmt(n)
}

function fmtPct(n) {
  if (n == null) return '—'
  const sign = n >= 0 ? '+' : ''
  return `${sign}${fmt(n)}%`
}

function PnlSpan({ value }) {
  if (value == null) return <span>—</span>
  const cls = value >= 0 ? 'pnl-positive' : 'pnl-negative'
  return <span className={cls}>{fmtMxn(value)}</span>
}

function Portfolio({ portfolio }) {
  if (!portfolio) return null

  const {
    initialCapital, mxnBalance, btcBalance, btcPrice, totalValueMxn,
    unrealizedPnl, totalReturn, totalReturnPct,
    totalTrades, winningTrades, winRate,
  } = portfolio

  return (
    <div className="portfolio card">
      <h2 className="portfolio-title">Portafolio Simulado</h2>
      <p className="portfolio-caption">Capital inicial: {fmtMxn(initialCapital)} MXN</p>

      <div className="portfolio-grid">
        <div className="portfolio-item">
          <span className="portfolio-label">MXN Disponible</span>
          <span className="portfolio-value">{fmtMxn(mxnBalance)}</span>
        </div>
        <div className="portfolio-item">
          <span className="portfolio-label">BTC en cartera</span>
          <span className="portfolio-value">{btcBalance != null ? btcBalance.toFixed(8) : '—'} BTC</span>
        </div>
        <div className="portfolio-item">
          <span className="portfolio-label">Valor total</span>
          <span className="portfolio-value">{fmtMxn(totalValueMxn)}</span>
        </div>
        <div className="portfolio-item">
          <span className="portfolio-label">P&L no realizado</span>
          <span className="portfolio-value"><PnlSpan value={unrealizedPnl} /></span>
        </div>
        <div className="portfolio-item">
          <span className="portfolio-label">Retorno total</span>
          <span className={`portfolio-value ${totalReturn >= 0 ? 'pnl-positive' : 'pnl-negative'}`}>
            {fmtMxn(totalReturn)} ({fmtPct(totalReturnPct)})
          </span>
        </div>
        <div className="portfolio-item">
          <span className="portfolio-label">Operaciones cerradas</span>
          <span className="portfolio-value">{totalTrades ?? 0}</span>
        </div>
        <div className="portfolio-item">
          <span className="portfolio-label">Operaciones ganadoras</span>
          <span className="portfolio-value">{winningTrades ?? 0}</span>
        </div>
        <div className="portfolio-item">
          <span className="portfolio-label">Tasa de éxito</span>
          <span className={`portfolio-value ${winRate >= 50 ? 'pnl-positive' : 'pnl-negative'}`}>
            {fmtPct(winRate)}
          </span>
        </div>
      </div>
    </div>
  )
}

export default Portfolio
